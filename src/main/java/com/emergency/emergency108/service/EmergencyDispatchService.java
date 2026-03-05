package com.emergency.emergency108.service;

import com.emergency.emergency108.dto.EmergencyUpdateDTO;
import com.emergency.emergency108.entity.*;
import com.emergency.emergency108.event.AssignmentEvent;
import com.emergency.emergency108.event.DomainEventPublisher;
import com.emergency.emergency108.exception.NoAmbulancesAvailableException;
import com.emergency.emergency108.repository.AmbulanceRepository;
import com.emergency.emergency108.repository.DriverSessionRepository;
import com.emergency.emergency108.repository.EmergencyAssignmentRepository;
import com.emergency.emergency108.repository.EmergencyRepository;
import com.emergency.emergency108.repository.UserRepository;
import com.emergency.emergency108.util.GeoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Service implementation for EmergencyDispatch operations.
 *
 * @author anupam kushwaha
 */
@Service
public class EmergencyDispatchService {

        private static final Logger log = LoggerFactory.getLogger(EmergencyDispatchService.class);

        private final AmbulanceRepository ambulanceRepository;
        private final DomainEventPublisher eventPublisher;
        private final EmergencyRepository emergencyRepository;
        private final EmergencyAssignmentRepository assignmentRepository;
        private final DriverSessionRepository driverSessionRepository;
        private final UserRepository userRepository;
        private final SimpMessagingTemplate messagingTemplate;
        private final FCMNotificationService fcmNotificationService;

        public EmergencyDispatchService(
                        AmbulanceRepository ambulanceRepository,
                        DomainEventPublisher eventPublisher,
                        EmergencyRepository emergencyRepository,
                        EmergencyAssignmentRepository assignmentRepository,
                        DriverSessionRepository driverSessionRepository,
                        UserRepository userRepository,
                        SimpMessagingTemplate messagingTemplate,
                        FCMNotificationService fcmNotificationService) {
                this.ambulanceRepository = ambulanceRepository;
                this.eventPublisher = eventPublisher;
                this.emergencyRepository = emergencyRepository;
                this.assignmentRepository = assignmentRepository;
                this.driverSessionRepository = driverSessionRepository;
                this.userRepository = userRepository;
                this.messagingTemplate = messagingTemplate;
                this.fcmNotificationService = fcmNotificationService;
        }

        /**
         * Dispatch emergency to nearest VERIFIED + ONLINE driver.
         * Driver-centric approach: Assigns to driver with active session, not just
         * ambulance.
         * 
         * @param emergencyId Emergency ID
         * @throws NoAmbulancesAvailableException if no verified online drivers
         *                                        available
         */
        @Transactional
        public void dispatchToNearestAvailableAmbulance(Long emergencyId) {
                Emergency emergency = emergencyRepository.findById(emergencyId)
                                .orElseThrow(() -> new IllegalArgumentException("Emergency not found: " + emergencyId));

                // Use heartbeat-tolerant query:
                // Accepts ONLINE sessions AND OFFLINE sessions with a recent REST heartbeat
                // (guards against Cloudflare/proxy killing STOMP WebSocket → session flips to
                // OFFLINE, but driver is still alive via REST PUT /driver/location heartbeats).
                LocalDateTime heartbeatCutoff = LocalDateTime.now().minusMinutes(2);
                List<DriverSession> onlineSessions = driverSessionRepository
                                .findAvailableDriversForDispatch(heartbeatCutoff);
                log.info("Found {} available sessions for dispatch (ONLINE or recent heartbeat)",
                                onlineSessions.size());

                // Auto-reactivate any OFFLINE-but-heartbeat-active sessions back to ONLINE
                // so their status is consistent and downstream checks work correctly.
                onlineSessions.forEach(session -> {
                        if (session.getStatus() == DriverSessionStatus.OFFLINE) {
                                log.warn("Driver {} session {} was OFFLINE but has recent heartbeat "
                                                + "— auto-reactivating to ONLINE for dispatch",
                                                session.getDriverId(), session.getId());
                                session.setStatus(DriverSessionStatus.ONLINE);
                                driverSessionRepository.save(session);
                        }
                });

                // Exclude drivers who have already rejected this emergency
                List<Long> rejectedDriverIds = assignmentRepository.findRejectedDriverIdsByEmergencyId(emergencyId);
                if (!rejectedDriverIds.isEmpty()) {
                        log.info("Excluding {} drivers who already rejected emergency {}", rejectedDriverIds.size(),
                                        emergencyId);
                        onlineSessions = onlineSessions.stream()
                                        .filter(session -> !rejectedDriverIds.contains(session.getDriverId()))
                                        .collect(Collectors.toList());
                }

                // STATUS CHECK: Must be CREATED before dispatch (Safety check)
                if (emergency.getStatus() != EmergencyStatus.CREATED) {
                        log.warn("Attempted to dispatch emergency {} but status is {} (must be CREATED)",
                                        emergencyId, emergency.getStatus());
                        throw new IllegalStateException(
                                        "Emergency can only be dispatched if status is CREATED. Current status: "
                                                        + emergency.getStatus());
                }

                List<DriverSession> eligibleSessions = onlineSessions;

                log.info("After filtering: {} eligible sessions (Window: 1 Hour)", eligibleSessions.size());

                if (eligibleSessions.isEmpty()) {
                        log.error("No available drivers found. Checked {} online sessions against 1-hour heartbeat window.",
                                        onlineSessions.size());
                        throw new NoAmbulancesAvailableException("No drivers available — no ONLINE verified drivers found");
                }

                // Find nearest driver
                DriverSession nearestSession = eligibleSessions.stream()
                                .min(Comparator.comparingDouble(session -> {
                                        if (session.getCurrentLat() == null || session.getCurrentLng() == null) {
                                                return Double.MAX_VALUE; // Skip drivers without location
                                        }
                                        return GeoUtil.distanceKm(
                                                        emergency.getLatitude(),
                                                        emergency.getLongitude(),
                                                        session.getCurrentLat(),
                                                        session.getCurrentLng());
                                }))
                                .orElseThrow(() -> new NoAmbulancesAvailableException(
                                                "No drivers with valid location"));

                // Get driver's ambulance
                Ambulance ambulance = ambulanceRepository.findByDriverId(nearestSession.getDriverId())
                                .orElseThrow(() -> new NoAmbulancesAvailableException(
                                                "Driver has no ambulance assigned"));

                // Create assignment
                EmergencyAssignment assignment = new EmergencyAssignment();
                assignment.setEmergency(emergency);
                assignment.setAmbulance(ambulance);
                assignment.setDriverId(nearestSession.getDriverId());
                assignment.setStatus(EmergencyAssignmentStatus.ASSIGNED);
                assignment.setAssignedAt(LocalDateTime.now());
                assignment.setResponseDeadline(LocalDateTime.now().plusMinutes(3)); // 3-minute response window: FCM can take 5-30s to arrive, driver needs time to unlock and respond
                assignmentRepository.save(assignment);

                // Update emergency status
                emergency.setStatus(EmergencyStatus.DISPATCHED);
                emergencyRepository.save(emergency);

                log.info("Emergency {} dispatched to driver {} (ambulance {})",
                                emergencyId, nearestSession.getDriverId(), ambulance.getId());

                eventPublisher.publish(
                                new AssignmentEvent(
                                                emergencyId,
                                                ambulance.getId(),
                                                "EMERGENCY_DISPATCHED",
                                                "Emergency dispatched to nearest verified driver"));

                broadcastEmergencyUpdate(emergency, nearestSession.getDriverId(), "DISPATCHED");

                // --- 1. Real-time STOMP Push (Foreground) ---
                Map<String, Object> assignmentPayload = new HashMap<>();
                assignmentPayload.put("assigned", true);

                Map<String, Object> emergencyMap = new HashMap<>();
                emergencyMap.put("id", emergency.getId());
                emergencyMap.put("type", emergency.getType());
                emergencyMap.put("severity", emergency.getSeverity());
                emergencyMap.put("latitude", emergency.getLatitude());
                emergencyMap.put("longitude", emergency.getLongitude());
                emergencyMap.put("userId", emergency.getUserId());
                emergencyMap.put("status", emergency.getStatus().name());
                assignmentPayload.put("emergency", emergencyMap);
                assignmentPayload.put("status", "ASSIGNED");

                messagingTemplate.convertAndSend("/topic/driver/" + nearestSession.getDriverId() + "/assignments",
                                assignmentPayload);

                // --- 2. High-Priority FCM Push (Background w/ Buzzer) ---
                User driverUser = userRepository.findById(nearestSession.getDriverId()).orElse(null);
                if (driverUser != null && driverUser.getFcmToken() != null && !driverUser.getFcmToken().isEmpty()) {
                        Map<String, String> data = new HashMap<>();
                        data.put("action", "NEW_EMERGENCY");
                        data.put("emergencyId", String.valueOf(emergency.getId()));

                        fcmNotificationService.sendPushNotification(
                                        driverUser.getFcmToken(),
                                        "🚨 New Emergency Assignment",
                                        "Tap to view patient details and accept/reject.",
                                        data);
                }
        }

        private void broadcastEmergencyUpdate(Emergency emergency, Long driverId, String eventName) {
                try {
                        String driverName = userRepository.findById(driverId)
                                        .map(User::getName)
                                        .orElse("Driver #" + driverId);

                        EmergencyUpdateDTO dto = new EmergencyUpdateDTO(
                                        emergency.getId(),
                                        emergency.getType(),
                                        emergency.getStatus().name(),
                                        emergency.getLatitude(),
                                        emergency.getLongitude(),
                                        driverId,
                                        driverName,
                                        eventName);

                        messagingTemplate.convertAndSend("/topic/emergency-updates", dto);
                        log.debug("Broadcast emergency-update: id={}, event={}", emergency.getId(), eventName);
                } catch (Exception e) {
                        log.warn("Failed to broadcast emergency update for {}: {}", emergency.getId(), e.getMessage());
                }
        }
}
