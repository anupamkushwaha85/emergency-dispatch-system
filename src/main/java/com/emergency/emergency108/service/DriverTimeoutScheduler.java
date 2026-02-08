package com.emergency.emergency108.service;

import com.emergency.emergency108.entity.Emergency;
import com.emergency.emergency108.entity.EmergencyAssignment;
import com.emergency.emergency108.entity.EmergencyAssignmentStatus;
import com.emergency.emergency108.repository.EmergencyAssignmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Scheduled job for driver response timeout monitoring.
 * Runs every 10 seconds to find ASSIGNED assignments past 60-second response deadline.
 * Auto-rejects timed-out assignments and re-dispatches to next driver.
 */
@Service
public class DriverTimeoutScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DriverTimeoutScheduler.class);

    private final EmergencyAssignmentRepository assignmentRepository;
    private final EmergencyService emergencyService;

    public DriverTimeoutScheduler(
            EmergencyAssignmentRepository assignmentRepository,
            EmergencyService emergencyService) {
        this.assignmentRepository = assignmentRepository;
        this.emergencyService = emergencyService;
    }

    /**
     * Check for timed-out driver assignments and handle them.
     * Runs every 10 seconds.
     */
    @Scheduled(fixedRate = 10000) // 10 seconds
    public void processTimedOutAssignments() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<EmergencyAssignment> timedOutAssignments = assignmentRepository.findTimedOutAssignments(now);

            if (!timedOutAssignments.isEmpty()) {
                logger.info("Found {} timed-out driver assignments", timedOutAssignments.size());

                for (EmergencyAssignment assignment : timedOutAssignments) {
                    try {
                        handleTimeout(assignment, now);
                    } catch (Exception e) {
                        logger.error("Error handling timeout for assignment {}: {}",
                                assignment.getId(), e.getMessage(), e);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error in driver timeout scheduler: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle timed-out assignment.
     * Mark as TIMEOUT and re-dispatch to next available driver.
     */
    private void handleTimeout(EmergencyAssignment assignment, LocalDateTime now) {
        Emergency emergency = assignment.getEmergency();
        Long driverId = assignment.getDriverId();
        
        logger.warn("Driver {} did not respond to emergency {} within deadline (assigned at: {}, deadline: {})",
                driverId, emergency.getId(), assignment.getAssignedAt(), assignment.getResponseDeadline());

        // Calculate response time (for analytics)
        if (assignment.getAssignedAt() != null && assignment.getResponseDeadline() != null) {
            long responseSeconds = ChronoUnit.SECONDS.between(assignment.getAssignedAt(), now);
            assignment.setResponseTimeSeconds((int) responseSeconds);
        }

        // Mark assignment as TIMEOUT
        assignment.setStatus(EmergencyAssignmentStatus.TIMEOUT);
        assignment.setCancelledAt(now);
        assignment.setCancellationReason("Driver did not respond within 60 seconds");
        assignmentRepository.save(assignment);

        logger.info("Marked assignment {} as TIMEOUT", assignment.getId());

        // Re-dispatch to next available driver
        try {
            logger.info("Re-dispatching emergency {} to next available driver", emergency.getId());
            emergencyService.dispatchToNearestDriver(emergency.getId());
        } catch (IllegalStateException e) {
            // Emergency already in different state - this is expected in concurrent scenarios
            logger.debug("Skipping re-dispatch for emergency {} - {}", emergency.getId(), e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to re-dispatch emergency {} after driver timeout: {}",
                    emergency.getId(), e.getMessage());
            // Emergency will remain in current status
        }
    }
}
