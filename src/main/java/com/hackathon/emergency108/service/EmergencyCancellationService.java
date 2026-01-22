package com.hackathon.emergency108.service;

import com.hackathon.emergency108.auth.security.AuthContext;
import com.hackathon.emergency108.entity.*;
import com.hackathon.emergency108.repository.EmergencyRepository;
import com.hackathon.emergency108.repository.EmergencyAssignmentRepository;
import com.hackathon.emergency108.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for handling emergency cancellations.
 * Implements early cancellation (within 100s) and late cancellation (after driver assigned) logic.
 */
@Service
public class EmergencyCancellationService {

    private static final Logger logger = LoggerFactory.getLogger(EmergencyCancellationService.class);

    private final EmergencyRepository emergencyRepository;
    private final EmergencyAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final DriverSessionService driverSessionService;
    private final EmergencyAuthorizationService authorizationService;

    public EmergencyCancellationService(
            EmergencyRepository emergencyRepository,
            EmergencyAssignmentRepository assignmentRepository,
            UserRepository userRepository,
            DriverSessionService driverSessionService,
            EmergencyAuthorizationService authorizationService) {
        this.emergencyRepository = emergencyRepository;
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
        this.driverSessionService = driverSessionService;
        this.authorizationService = authorizationService;
    }

    /**
     * Cancel emergency by user.
     * Determines if it's an early cancellation (within 100s) or late cancellation (after driver assigned).
     * 
     * @param emergencyId Emergency ID
     * @param userId User ID (must be emergency creator)
     * @return Cancellation result
     * @throws IllegalArgumentException if emergency not found or user not authorized
     */
    @Transactional
    public CancellationResult cancelEmergency(Long emergencyId, Long userId) {
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
            return handleLateCancellation(emergency, userId);
        }
    }

    /**
     * Handle early cancellation (within 100 seconds, no driver assigned).
     * No penalty for user.
     */
    private CancellationResult handleEarlyCancellation(Emergency emergency, Long userId) {
        logger.info("Early cancellation by user {} for emergency {}", userId, emergency.getId());

        emergency.setStatus(EmergencyStatus.CANCELLED);
        emergency.setIsSuspectCancellation(false);
        emergencyRepository.saveAndFlush(emergency);  // CRITICAL: Flush to DB immediately
        
        logger.info("Emergency {} status updated to CANCELLED in database", emergency.getId());

        return new CancellationResult(
                true,
                "Emergency cancelled successfully",
                false,
                "Early cancellation - no penalty"
        );
    }

    /**
     * Handle late cancellation (after 100 seconds or driver already assigned).
     * User is marked as suspect.
     */
    private CancellationResult handleLateCancellation(Emergency emergency, Long userId) {
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
            assignment.setStatus(EmergencyAssignmentStatus.CANCELLED_BY_USER);
            assignment.setCancelledAt(LocalDateTime.now());
            assignment.setCancellationReason("User cancelled emergency after driver assigned");
            assignmentRepository.save(assignment);
        }

        // Mark emergency as suspect cancellation
        emergency.setStatus(EmergencyStatus.CANCELLED);
        emergency.setIsSuspectCancellation(true);
        emergencyRepository.save(emergency);

        // Mark user as suspect
        markUserAsSuspect(userId);

        return new CancellationResult(
                true,
                "Emergency cancelled, but you cancelled after driver was assigned",
                true,
                "Late cancellation - marked as suspect"
        );
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

                // Update ambulance status if needed
                Ambulance ambulance = assignment.getAmbulance();
                if (ambulance != null && ambulance.getStatus() == AmbulanceStatus.BUSY) {
                    ambulance.setStatus(AmbulanceStatus.AVAILABLE);
                    // Save ambulance through repository (assuming it's managed)
                    logger.info("Ambulance {} set to AVAILABLE", ambulance.getId());
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

            // TODO: Implement penalty logic (e.g., temporary suspension after 3 suspect cancellations)
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
        return assignmentRepository.findByEmergencyIdAndStatus(emergencyId, EmergencyAssignmentStatus.ASSIGNED).isPresent() ||
                assignmentRepository.findByEmergencyIdAndStatus(emergencyId, EmergencyAssignmentStatus.ACCEPTED).isPresent();
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

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public boolean isSuspect() {
            return isSuspect;
        }

        public String getPenaltyReason() {
            return penaltyReason;
        }
    }
}
