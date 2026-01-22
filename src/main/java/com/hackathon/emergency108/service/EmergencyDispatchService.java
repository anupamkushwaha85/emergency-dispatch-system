package com.hackathon.emergency108.service;

import com.hackathon.emergency108.entity.*;
import com.hackathon.emergency108.event.AssignmentEvent;
import com.hackathon.emergency108.event.DomainEventPublisher;
import com.hackathon.emergency108.exception.NoAmbulancesAvailableException;
import com.hackathon.emergency108.repository.AmbulanceRepository;
import com.hackathon.emergency108.repository.DriverSessionRepository;
import com.hackathon.emergency108.repository.EmergencyAssignmentRepository;
import com.hackathon.emergency108.repository.EmergencyRepository;
import com.hackathon.emergency108.util.GeoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmergencyDispatchService {

        private static final Logger log = LoggerFactory.getLogger(EmergencyDispatchService.class);

        private final AmbulanceRepository ambulanceRepository;
        private final DomainEventPublisher eventPublisher;
        private final EmergencyRepository emergencyRepository;
        private final EmergencyAssignmentRepository assignmentRepository;
        private final DriverSessionRepository driverSessionRepository;

        public EmergencyDispatchService(
                        AmbulanceRepository ambulanceRepository,
                        DomainEventPublisher eventPublisher,
                        EmergencyRepository emergencyRepository,
                        EmergencyAssignmentRepository assignmentRepository,
                        DriverSessionRepository driverSessionRepository) {
                this.ambulanceRepository = ambulanceRepository;
                this.eventPublisher = eventPublisher;
                this.emergencyRepository = emergencyRepository;
                this.assignmentRepository = assignmentRepository;
                this.driverSessionRepository = driverSessionRepository;
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

                // Find VERIFIED + ONLINE drivers with recent heartbeat
                List<DriverSession> onlineSessions = driverSessionRepository.findAllOnlineDrivers();
                log.info("Found {} online sessions from query", onlineSessions.size());

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

                // Filter for VERIFIED drivers with fresh heartbeat (< 1 hour for testing)
                LocalDateTime thirtySecondsAgo = LocalDateTime.now().minusSeconds(3600);
                List<DriverSession> eligibleSessions = onlineSessions.stream()
                                .filter(session -> {
                                        if (session.getLastHeartbeat() == null) {
                                                log.warn("Session {} has null heartbeat - filtering out",
                                                                session.getId());
                                                return false;
                                        }
                                        if (!session.getLastHeartbeat().isAfter(thirtySecondsAgo)) {
                                                log.warn("Session {} heartbeat is stale ({}) - filtering out",
                                                                session.getId(), session.getLastHeartbeat());
                                                return false;
                                        }
                                        return true;
                                })
                                .filter(session -> {
                                        if (session.getStatus() != DriverSessionStatus.ONLINE) {
                                                log.warn("Session {} status is {} not ONLINE - filtering out",
                                                                session.getId(), session.getStatus());
                                                return false;
                                        }
                                        return true;
                                })
                                .collect(Collectors.toList());

                log.info("After filtering: {} eligible sessions available for dispatch", eligibleSessions.size());

                if (eligibleSessions.isEmpty()) {
                        log.error("No VERIFIED + ONLINE drivers available for emergency {}. " +
                                        "Query returned {} sessions, but all filtered out due to: " +
                                        "null heartbeat, stale heartbeat (>30s), or non-ONLINE status",
                                        emergencyId, onlineSessions.size());
                        throw new NoAmbulancesAvailableException("No verified online drivers available");
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
                assignment.setResponseDeadline(LocalDateTime.now().plusSeconds(60)); // 60 second response deadline
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
        }

}
