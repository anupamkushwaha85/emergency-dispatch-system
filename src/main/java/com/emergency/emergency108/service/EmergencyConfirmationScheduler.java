package com.emergency.emergency108.service;

import com.emergency.emergency108.entity.Emergency;
import com.emergency.emergency108.entity.EmergencyStatus;
import com.emergency.emergency108.repository.EmergencyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job for emergency confirmation deadline monitoring.
 * Runs every 10 seconds to find CREATED emergencies past 100-second deadline and auto-dispatch them.
 */
@Service
public class EmergencyConfirmationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(EmergencyConfirmationScheduler.class);

    private final EmergencyRepository emergencyRepository;
    private final EmergencyService emergencyService;

    public EmergencyConfirmationScheduler(
            EmergencyRepository emergencyRepository,
            EmergencyService emergencyService) {
        this.emergencyRepository = emergencyRepository;
        this.emergencyService = emergencyService;
    }

    /**
     * Check for unconfirmed emergencies and auto-dispatch them.
     * Runs every 10 seconds.
     */
    @Scheduled(fixedRate = 10000) // 10 seconds
    public void processUnconfirmedEmergencies() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<Emergency> unconfirmedEmergencies = emergencyRepository.findUnconfirmedEmergencies(now);

            if (!unconfirmedEmergencies.isEmpty()) {
                logger.info("Found {} unconfirmed emergencies to auto-dispatch", unconfirmedEmergencies.size());

                for (Emergency emergency : unconfirmedEmergencies) {
                    try {
                        autoDispatchEmergency(emergency);
                    } catch (Exception e) {
                        logger.error("Error auto-dispatching emergency {}: {}",
                                emergency.getId(), e.getMessage(), e);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error in emergency confirmation scheduler: {}", e.getMessage(), e);
        }
    }

    /**
     * Auto-dispatch an emergency that passed confirmation deadline.
     */
    private void autoDispatchEmergency(Emergency emergency) {
        logger.info("Auto-dispatching emergency {} (created at: {}, deadline: {})",
                emergency.getId(), emergency.getCreatedAt(), emergency.getConfirmationDeadline());

        try {
            // CRITICAL: Reload emergency from DB to get latest status (may have been cancelled)
            Emergency freshEmergency = emergencyRepository.findById(emergency.getId())
                    .orElseThrow(() -> new IllegalStateException("Emergency not found"));
            
            if (freshEmergency.getStatus() != EmergencyStatus.CREATED) {
                logger.debug("Emergency {} status changed to {} - skipping dispatch", 
                        emergency.getId(), freshEmergency.getStatus());
                return;
            }
            
            // Dispatch to nearest available driver
            emergencyService.dispatchToNearestDriver(emergency.getId());
            logger.info("Successfully auto-dispatched emergency {}", emergency.getId());
        } catch (IllegalStateException e) {
            // Emergency already dispatched or in wrong state - this is expected in concurrent scenarios
            logger.debug("Skipping emergency {} - {}", emergency.getId(), e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to auto-dispatch emergency {}: {}", emergency.getId(), e.getMessage());
            // Emergency remains in CREATED status - will be retried in next cycle
        }
    }
}
