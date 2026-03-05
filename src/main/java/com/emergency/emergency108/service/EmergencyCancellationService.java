package com.emergency.emergency108.service;

import com.emergency.emergency108.auth.security.AuthContext;
import com.emergency.emergency108.entity.*;
import com.emergency.emergency108.repository.AmbulanceRepository;
import com.emergency.emergency108.repository.EmergencyRepository;
import com.emergency.emergency108.repository.EmergencyAssignmentRepository;
import com.emergency.emergency108.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import com.emergency.emergency108.dto.EmergencyUpdateDTO;

/**
 * Service for handling emergency cancellations.
 * Implements early cancellation (within 100s) and late cancellation (after
 * driver assigned) logic.
 */
/**
 * Service implementation for EmergencyCancellation operations.
 *
 * @author anupam kushwaha
 */
@Service
public class EmergencyCancellationService {

    private static final Logger logger = LoggerFactory.getLogger(EmergencyCancellationService.class);

    private final EmergencyRepository emergencyRepository;
    private final EmergencyAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final DriverSessionService driverSessionService;
    private final EmergencyAuthorizationService authorizationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final AmbulanceRepository ambulanceRepository;

    public EmergencyCancellationService(
            EmergencyRepository emergencyRepository,
            EmergencyAssignmentRepository assignmentRepository,
            UserRepository userRepository,
            DriverSessionService driverSessionService,
            EmergencyAuthorizationService authorizationService,
            SimpMessagingTemplate messagingTemplate,
            AmbulanceRepository ambulanceRepository) {
        this.emergencyRepository = emergencyRepository;
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
        this.driverSessionService = driverSessionService;
        this.authorizationService = authorizationService;
        this.messagingTemplate = messagingTemplate;
        this.ambulanceRepository = ambulanceRepository;
    }

    /**
     * Cancel emergency by user.
     * Determines if it's an early cancellation (within 100s) or late cancellation
     * (after driver assigned).
     * 
     * @param emergencyId Emergency ID
     * @param userId      User ID (must be emergency creator)
     * @return Cancellation result
     * @throws IllegalArgumentException if emergency not found or user not
     *                                  authorized
     */
    @Transactional
    public CancellationResult cancelEmergency(Long emergencyId, Long userId, String reason) {
        // Find emergency
        Optional<Emergency> emergencyOpt = emergencyRepository.findById(emergencyId);
        if (emergencyOpt.isEmpty()) {
            throw new IllegalArgumentException("Emergency not found: " + emergencyId);
        }

        Emergency emergency = emergencyOpt.get();

        // Authorization check: User must be the creator
        if (!authorizationService.canUserCancelEmergency(userId, emergency)) {
            throw new IllegalArgumentException("User not authorized to cancel this emergency");
        }

        // Check current status
        EmergencyStatus currentStatus = emergency.getStatus();
        if (currentStatus == EmergencyStatus.COMPLETED || currentStatus == EmergencyStatus.CANCELLED) {
            throw new IllegalStateException("Emergency already " + currentStatus);
        }

        LocalDateTime now = LocalDateTime.now();
        boolean isEarlyCancellation = isWithinConfirmationWindow(emergency, now);
        boolean hasDriverAssigned = hasActiveDriverAssignment(emergencyId);

        if (isEarlyCancellation && !hasDriverAssigned) {
            // EARLY CANCELLATION: Within 100s and no driver assigned yet
            return handleEarlyCancellation(emergency, userId);
        } else {
            // LATE CANCELLATION: After 100s or driver already assigned
            return handleLateCancellation(emergency, userId, reason);
        }
    }

    /**
     * Cancel emergency as admin. Bypasses user-creator authorization.
     * Releases any assigned driver, marks emergency cancelled, broadcasts update.
     * No suspect penalty is applied to the user.
     *
     * @param emergencyId Emergency ID
     * @param adminId     Admin user ID (for logging)
     * @param reason      Reason for cancellation (e.g. "Fake emergency")
     * @return Cancellation result
     */
    @Transactional
    public CancellationResult adminCancelEmergency(Long emergencyId, Long adminId, String reason) {
        Emergency emergency = emergencyRepository.findById(emergencyId)
                .orElseThrow(() -> new IllegalArgumentException("Emergency not found: " + emergencyId));

        EmergencyStatus currentStatus = emergency.getStatus();
        if (currentStatus == EmergencyStatus.COMPLETED || currentStatus == EmergencyStatus.CANCELLED) {
            throw new IllegalStateException("Emergency already " + currentStatus);
        }

        // Release any active driver assignment (but do NOT mark user as suspect)
        Optional<EmergencyAssignment> activeAssignmentOpt = assignmentRepository
                .findByEmergencyIdAndStatus(emergency.getId(), EmergencyAssignmentStatus.ASSIGNED);
        if (activeAssignmentOpt.isEmpty()) {
            activeAssignmentOpt = assignmentRepository
                    .findByEmergencyIdAndStatus(emergency.getId(), EmergencyAssignmentStatus.ACCEPTED);
        }
        if (activeAssignmentOpt.isPresent()) {
            EmergencyAssignment assignment = activeAssignmentOpt.get();
            String cancelReason = reason != null && !reason.isBlank() ? reason : "Cancelled by admin";
            assignment.setCancellationReason(cancelReason);
            releaseDriver(assignment);
            assignment.setStatus(EmergencyAssignmentStatus.CANCELLED);
            assignment.setCancelledAt(LocalDateTime.now());
            assignmentRepository.save(assignment);
        }

        emergency.setStatus(EmergencyStatus.CANCELLED);
        emergency.setIsSuspectCancellation(false); // admin cancel — no user penalty
        emergencyRepository.saveAndFlush(emergency);

        broadcastEmergencyUpdate(emergency, null, "EMERGENCY_CANCELLED");
        logger.info("Admin {} force-cancelled emergency {} — reason: {}", adminId, emergencyId, reason);

        return new CancellationResult(true, "Emergency cancelled by admin", false,
                reason != null ? reason : "Cancelled by admin");
    }

    /**
     * Cancel an active mission by the assigned driver.
     * Releases the driver back to ONLINE, ambulance to AVAILABLE, and notifies the patient.
     */
    @Transactional
    public CancellationResult driverCancelMission(Long emergencyId, Long driverId) {
        Emergency emergency = emergencyRepository.findById(emergencyId)
                .orElseThrow(() -> new IllegalArgumentException("Emergency not found: " + emergencyId));

        if (emergency.getStatus() == EmergencyStatus.COMPLETED
                || emergency.getStatus() == EmergencyStatus.CANCELLED) {
            throw new IllegalArgumentException(
                    "Emergency is already in a terminal state: " + emergency.getStatus());
        }

        // Find the active assignment (try ACCEPTED first, then ASSIGNED)
        Optional<EmergencyAssignment> assignmentOpt = assignmentRepository
                .findByEmergencyIdAndStatus(emergencyId, EmergencyAssignmentStatus.ACCEPTED);
        if (assignmentOpt.isEmpty()) {
            assignmentOpt = assignmentRepository
                    .findByEmergencyIdAndStatus(emergencyId, EmergencyAssignmentStatus.ASSIGNED);
        }
        EmergencyAssignment assignment = assignmentOpt
                .orElseThrow(() -> new IllegalArgumentException(
                        "No active assignment found for emergency: " + emergencyId));

        if (!driverId.equals(assignment.getDriverId())) {
            throw new SecurityException("Driver " + driverId
                    + " does not own the assignment for emergency " + emergencyId);
        }

        // Cancel the assignment
        assignment.setStatus(EmergencyAssignmentStatus.CANCELLED);
        assignment.setCancelledAt(LocalDateTime.now());
        assignment.setCancellationReason("Driver cancelled mission");
        assignmentRepository.save(assignment);

        // Cancel the emergency
        emergency.setStatus(EmergencyStatus.CANCELLED);
        emergencyRepository.save(emergency);

        // Release driver → ONLINE, ambulance → AVAILABLE, notify driver frontend
        releaseDriver(assignment);

        // Notify patient/user via WebSocket broadcast
        broadcastEmergencyUpdate(emergency, null, "EMERGENCY_CANCELLED");

        logger.warn("Driver {} cancelled active mission for emergency {}", driverId, emergencyId);
        return new CancellationResult(true, "Mission cancelled by driver", false, null);
    }

    /**
     * Handle early cancellation (within 100 seconds, no driver assigned).
     * No penalty for user.
     */
    private CancellationResult handleEarlyCancellation(Emergency emergency, Long userId) {
        logger.info("Early cancellation by user {} for emergency {}", userId, emergency.getId());

        emergency.setStatus(EmergencyStatus.CANCELLED);
        emergency.setIsSuspectCancellation(false);
        emergencyRepository.saveAndFlush(emergency); // CRITICAL: Flush to DB immediately

        logger.info("Emergency {} status updated to CANCELLED in database", emergency.getId());
        broadcastEmergencyUpdate(emergency, null, "EMERGENCY_CANCELLED");

        return new CancellationResult(
                true,
                "Emergency cancelled successfully",
                false,
                "Early cancellation - no penalty");
    }

    /**
     * Handle late cancellation (after 100 seconds or driver already assigned).
     * User is marked as suspect.
     */
    private CancellationResult handleLateCancellation(Emergency emergency, Long userId, String reason) {
        logger.warn("Late cancellation by user {} for emergency {} (status: {})",
                userId, emergency.getId(), emergency.getStatus());

        // Find active assignment and release driver
        Optional<EmergencyAssignment> activeAssignmentOpt = assignmentRepository
                .findByEmergencyIdAndStatus(emergency.getId(), EmergencyAssignmentStatus.ASSIGNED);

        if (activeAssignmentOpt.isEmpty()) {
            activeAssignmentOpt = assignmentRepository
                    .findByEmergencyIdAndStatus(emergency.getId(), EmergencyAssignmentStatus.ACCEPTED);
        }

        if (activeAssignmentOpt.isPresent()) {
            EmergencyAssignment assignment = activeAssignmentOpt.get();
            releaseDriver(assignment);

            // Mark assignment as cancelled
            assignment.setStatus(EmergencyAssignmentStatus.CANCELLED);
            assignment.setCancelledAt(LocalDateTime.now());
            // Use provided reason or fallback
            assignment.setCancellationReason(reason != null && !reason.isBlank()
                    ? reason
                    : "User cancelled emergency after driver assigned");
            assignmentRepository.save(assignment);
        }

        // Mark emergency as suspect cancellation
        emergency.setStatus(EmergencyStatus.CANCELLED);
        emergency.setIsSuspectCancellation(true);
        emergencyRepository.save(emergency);

        broadcastEmergencyUpdate(emergency, null, "EMERGENCY_CANCELLED");

        // Mark user as suspect
        markUserAsSuspect(userId);

        return new CancellationResult(
                true,
                "Emergency cancelled, but you cancelled after driver was assigned",
                true,
                "Late cancellation - marked as suspect");
    }

    /**
     * Release driver from assignment and set them back to ONLINE status.
     */
    private void releaseDriver(EmergencyAssignment assignment) {
        Long driverId = assignment.getDriverId();
        if (driverId != null) {
            try {
                DriverSession session = driverSessionService.getActiveSession(driverId);
                if (session != null && session.getStatus() == DriverSessionStatus.ON_TRIP) {
                    session.setStatus(DriverSessionStatus.ONLINE);
                    driverSessionService.saveSession(session);
                    logger.info("Driver {} released back to ONLINE status", driverId);
                }

                // Notify the driver frontend of the cancellation
                Map<String, Object> payload = new HashMap<>();
                payload.put("assigned", false);
                payload.put("reason", assignment.getCancellationReason() != null ? assignment.getCancellationReason()
                        : "Mission Cancelled by User");
                messagingTemplate.convertAndSend("/topic/driver/" + driverId + "/assignments", payload);

                // Update ambulance status if needed
                Ambulance ambulance = assignment.getAmbulance();
                if (ambulance != null && ambulance.getStatus() == AmbulanceStatus.BUSY) {
                    ambulance.setStatus(AmbulanceStatus.AVAILABLE);
                    ambulanceRepository.save(ambulance); // persist — was missing before
                    logger.info("Ambulance {} set to AVAILABLE after cancellation", ambulance.getId());
                }
            } catch (Exception e) {
                logger.error("Error releasing driver {}: {}", driverId, e.getMessage());
            }
        }
    }

    /**
     * Increment suspect count for user.
     */
    private void markUserAsSuspect(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            int currentCount = user.getSuspectCount() != null ? user.getSuspectCount() : 0;
            user.setSuspectCount(currentCount + 1);
            user.setLastSuspectAt(LocalDateTime.now());
            userRepository.save(user);

            logger.warn("User {} marked as suspect (total suspect count: {})", userId, user.getSuspectCount());

            // TODO: Implement penalty logic (e.g., temporary suspension after 3 suspect
            // cancellations)
            if (user.getSuspectCount() >= 3) {
                logger.error("User {} has {} suspect cancellations - consider suspension",
                        userId, user.getSuspectCount());
            }
        }
    }

    /**
     * Check if emergency is within confirmation window (100 seconds).
     */
    private boolean isWithinConfirmationWindow(Emergency emergency, LocalDateTime now) {
        return emergency.getConfirmationDeadline() != null &&
                now.isBefore(emergency.getConfirmationDeadline());
    }

    /**
     * Check if emergency has active driver assignment.
     */
    private boolean hasActiveDriverAssignment(Long emergencyId) {
        return assignmentRepository.findByEmergencyIdAndStatus(emergencyId, EmergencyAssignmentStatus.ASSIGNED)
                .isPresent() ||
                assignmentRepository.findByEmergencyIdAndStatus(emergencyId, EmergencyAssignmentStatus.ACCEPTED)
                        .isPresent();
    }

    private void broadcastEmergencyUpdate(Emergency emergency, Long driverId, String eventName) {
        try {
            String driverName = null;
            if (driverId != null) {
                driverName = userRepository.findById(driverId)
                        .map(User::getName)
                        .orElse("Driver #" + driverId);
            }

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
            logger.debug("Broadcast emergency-update for cancellation: id={}, event={}", emergency.getId(), eventName);
        } catch (Exception e) {
            logger.warn("Failed to broadcast emergency update for cancellation {}: {}", emergency.getId(),
                    e.getMessage());
        }
    }

    /**
     * Result of cancellation operation.
     */
    public static class CancellationResult {
        private final boolean success;
        private final String message;
        private final boolean isSuspect;
        private final String penaltyReason;

        public CancellationResult(boolean success, String message, boolean isSuspect, String penaltyReason) {
            this.success = success;
            this.message = message;
            this.isSuspect = isSuspect;
            this.penaltyReason = penaltyReason;
        }

    /**
     * Is success operation.
     * @return the boolean
     */
        public boolean isSuccess() {
            return success;
        }

    /**
     * Get message operation.
     * @return the String
     */
        public String getMessage() {
            return message;
        }

    /**
     * Is suspect operation.
     * @return the boolean
     */
        public boolean isSuspect() {
            return isSuspect;
        }

    /**
     * Get penalty reason operation.
     * @return the String
     */
        public String getPenaltyReason() {
            return penaltyReason;
        }
    }
}
