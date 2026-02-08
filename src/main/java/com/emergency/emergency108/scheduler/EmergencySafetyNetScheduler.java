package com.emergency.emergency108.scheduler;

import com.emergency.emergency108.entity.ContactNotificationStatus;
import com.emergency.emergency108.entity.Emergency;
import com.emergency.emergency108.entity.EmergencyFor;
import com.emergency.emergency108.repository.EmergencyRepository;
import com.emergency.emergency108.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class EmergencySafetyNetScheduler {

    private static final Logger log = LoggerFactory.getLogger(EmergencySafetyNetScheduler.class);

    private final EmergencyRepository emergencyRepository;
    private final NotificationService notificationService;

    public EmergencySafetyNetScheduler(EmergencyRepository emergencyRepository,
            NotificationService notificationService) {
        this.emergencyRepository = emergencyRepository;
        this.notificationService = notificationService;
    }

    /**
     * Safety Net: Runs every 10 seconds.
     * Checks for emergencies where the 30s ownership decision window has expired.
     * Defaults them to SELF and triggers notification.
     */
    @Scheduled(fixedRate = 10000)
    @Transactional
    public void processOwnershipTimeouts() {
        LocalDateTime now = LocalDateTime.now();
        List<Emergency> timedOutEmergencies = emergencyRepository.findPendingOwnershipTimeouts(now);

        if (!timedOutEmergencies.isEmpty()) {
            log.info("Found {} emergencies with ownership timeout. Processing Safety Net...",
                    timedOutEmergencies.size());
        }

        for (Emergency emergency : timedOutEmergencies) {
            try {
                processTimeout(emergency);
            } catch (Exception e) {
                log.error("Failed to process timeout for emergency {}", emergency.getId(), e);
            }
        }
    }

    private void processTimeout(Emergency emergency) {
        log.info("SAFETY NET: Emergency {} passed 30s deadline. Auto-defaulting to SELF.", emergency.getId());

        // Default to SELF
        emergency.setEmergencyFor(EmergencyFor.SELF);

        // Notify
        notificationService.notifyContacts(emergency);

        // NotificationService updates the status, but we ensure it's saved
        emergencyRepository.save(emergency);
    }
}
