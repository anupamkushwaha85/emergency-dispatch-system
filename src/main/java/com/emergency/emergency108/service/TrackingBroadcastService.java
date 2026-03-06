package com.emergency.emergency108.service;

import com.emergency.emergency108.entity.DriverSession;
import com.emergency.emergency108.entity.Emergency;
import com.emergency.emergency108.entity.EmergencyAssignment;
import com.emergency.emergency108.entity.EmergencyAssignmentStatus;
import com.emergency.emergency108.entity.Hospital;
import com.emergency.emergency108.repository.DriverSessionRepository;
import com.emergency.emergency108.repository.EmergencyAssignmentRepository;
import com.emergency.emergency108.repository.EmergencyRepository;
import com.emergency.emergency108.util.GeoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Broadcasts real-time patient tracking payloads to the Flutter patient app
 * via STOMP on topic {@code /topic/emergency/{id}/tracking}.
 *
 * <p>Two entry points:
 * <ol>
 *   <li>{@link #broadcastForDriver(Long, double, double)} — called from
 *       {@link DriverSessionService#updateLocation} on every GPS tick so the
 *       patient map moves in real-time without polling.</li>
 *   <li>{@link #broadcastForEmergency(Long)} — called on status transitions
 *       (AT_PATIENT, TO_HOSPITAL, COMPLETED) so the patient timeline updates
 *       instantly.</li>
 * </ol>
 *
 * The payload shape is identical to the REST {@code GET /api/emergencies/{id}/track}
 * endpoint so the Flutter {@code EmergencyTrackingView} widget requires no changes.
 */
@Service
public class TrackingBroadcastService {

    private static final Logger log = LoggerFactory.getLogger(TrackingBroadcastService.class);

    private static final String TOPIC_PREFIX = "/topic/emergency/";
    private static final String TOPIC_SUFFIX = "/tracking";

    private final EmergencyAssignmentRepository assignmentRepository;
    private final EmergencyRepository emergencyRepository;
    private final DriverSessionRepository sessionRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public TrackingBroadcastService(
            EmergencyAssignmentRepository assignmentRepository,
            EmergencyRepository emergencyRepository,
            DriverSessionRepository sessionRepository,
            SimpMessagingTemplate messagingTemplate) {
        this.assignmentRepository = assignmentRepository;
        this.emergencyRepository = emergencyRepository;
        this.sessionRepository = sessionRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Called on every driver GPS update.  Looks up the driver's ACCEPTED
     * assignment, builds the tracking payload with the fresh coordinates, and
     * pushes it to the patient's STOMP subscription.
     *
     * @param driverId current driver
     * @param driverLat fresh latitude
     * @param driverLng fresh longitude
     */
    public void broadcastForDriver(Long driverId, double driverLat, double driverLng) {
        try {
            Optional<EmergencyAssignment> assignmentOpt =
                    assignmentRepository.findByDriverIdAndStatus(driverId, EmergencyAssignmentStatus.ACCEPTED);
            if (assignmentOpt.isEmpty()) return; // driver not on active trip

            EmergencyAssignment assignment = assignmentOpt.get();
            Emergency emergency = assignment.getEmergency();
            if (emergency == null) return;

            Map<String, Object> payload = buildPayload(emergency, assignment, driverLat, driverLng);
            String topic = TOPIC_PREFIX + emergency.getId() + TOPIC_SUFFIX;
            messagingTemplate.convertAndSend(topic, payload);
            log.debug("Tracking broadcast → {} driver=({},{})", topic, driverLat, driverLng);
        } catch (Exception e) {
            // Never let a broadcast failure crash a GPS update
            log.warn("trackingBroadcast for driver {} failed: {}", driverId, e.getMessage());
        }
    }

    /**
     * Called on status transitions (AT_PATIENT, TO_HOSPITAL, COMPLETED, IN_PROGRESS).
     * Loads the driver's last known GPS from the session and pushes the updated
     * payload so the patient timeline reflects the new status instantly.
     *
     * @param emergencyId the emergency whose status just changed
     */
    public void broadcastForEmergency(Long emergencyId) {
        try {
            Emergency emergency = emergencyRepository.findById(emergencyId).orElse(null);
            if (emergency == null) return;

            Optional<EmergencyAssignment> assignmentOpt =
                    assignmentRepository.findActiveAssignmentByEmergencyId(emergencyId);

            // If no active ASSIGNED/ACCEPTED assignment (e.g. just after COMPLETED),
            // fall back to the most recent assignment record.
            if (assignmentOpt.isEmpty()) {
                assignmentOpt = assignmentRepository.findTopByEmergencyIdOrderByAssignedAtDesc(emergencyId);
            }

            if (assignmentOpt.isEmpty()) {
                // No assignment at all — send status-only payload (edge case)
                Map<String, Object> bare = new HashMap<>();
                bare.put("emergencyId", emergencyId);
                bare.put("status", emergency.getStatus().toString());
                bare.put("patientLat", emergency.getLatitude());
                bare.put("patientLng", emergency.getLongitude());
                bare.put("emergencyType", emergency.getType());
                messagingTemplate.convertAndSend(TOPIC_PREFIX + emergencyId + TOPIC_SUFFIX, bare);
                return;
            }

            EmergencyAssignment assignment = assignmentOpt.get();

            // Resolve driver GPS from session (last known location)
            double driverLat = 0, driverLng = 0;
            if (assignment.getDriverId() != null) {
                Optional<DriverSession> sessionOpt =
                        sessionRepository.findActiveSessionByDriverId(assignment.getDriverId());
                if (sessionOpt.isPresent() &&
                        sessionOpt.get().getCurrentLat() != null &&
                        sessionOpt.get().getCurrentLng() != null) {
                    driverLat = sessionOpt.get().getCurrentLat();
                    driverLng = sessionOpt.get().getCurrentLng();
                }
            }

            Map<String, Object> payload = buildPayload(emergency, assignment, driverLat, driverLng);
            String topic = TOPIC_PREFIX + emergencyId + TOPIC_SUFFIX;
            messagingTemplate.convertAndSend(topic, payload);
            log.info("Tracking broadcast on status {} → {}", emergency.getStatus(), topic);
        } catch (Exception e) {
            log.warn("trackingBroadcast for emergency {} failed: {}", emergencyId, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private Map<String, Object> buildPayload(
            Emergency emergency,
            EmergencyAssignment assignment,
            double driverLat,
            double driverLng) {

        Map<String, Object> payload = new HashMap<>();
        payload.put("emergencyId", emergency.getId());
        payload.put("status", emergency.getStatus().toString());
        payload.put("patientLat", emergency.getLatitude());
        payload.put("patientLng", emergency.getLongitude());
        payload.put("emergencyType", emergency.getType());
        payload.put("severity", emergency.getSeverity());
        payload.put("ambulanceAssigned", true);

        // Ambulance code (for display in tracking view)
        if (assignment.getAmbulance() != null) {
            payload.put("ambulanceCode", assignment.getAmbulance().getCode());
        }

        // Driver GPS (non-zero = live location available)
        if (driverLat != 0 || driverLng != 0) {
            payload.put("driverLat", driverLat);
            payload.put("driverLng", driverLng);

            // Distance and ETA (same formula as REST endpoint)
            double distanceKm = GeoUtil.distanceKm(
                    driverLat, driverLng,
                    emergency.getLatitude(), emergency.getLongitude());
            int etaMinutes = (int) Math.ceil(distanceKm / 0.5); // 30 km/h
            payload.put("distanceKm", Math.round(distanceKm * 100.0) / 100.0);
            payload.put("etaMinutes", etaMinutes);
        }

        // Hospital info (present once TO_HOSPITAL or COMPLETED)
        Hospital hospital = assignment.getDestinationHospital();
        if (hospital != null) {
            payload.put("hospitalLat", hospital.getLatitude());
            payload.put("hospitalLng", hospital.getLongitude());
            payload.put("hospitalName", hospital.getName());
        }

        // Human-readable message matching REST endpoint behaviour
        switch (emergency.getStatus()) {
            case AT_PATIENT:
                payload.put("message", "Driver has arrived at your location");
                break;
            case TO_HOSPITAL:
                payload.put("message", "Transporting patient to hospital");
                break;
            case COMPLETED:
                payload.put("message", "Mission completed — patient delivered");
                break;
            default:
                payload.put("message", "Ambulance en route");
        }

        return payload;
    }
}
