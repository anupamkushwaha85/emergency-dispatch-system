package com.hackathon.emergency108.service;

import com.hackathon.emergency108.entity.ContactNotificationStatus;
import com.hackathon.emergency108.entity.Emergency;
import com.hackathon.emergency108.entity.EmergencyContact;
import com.hackathon.emergency108.repository.EmergencyContactRepository;
import com.hackathon.emergency108.repository.EmergencyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final EmergencyContactRepository contactRepository;
    private final EmergencyRepository emergencyRepository;

    public NotificationService(EmergencyContactRepository contactRepository, EmergencyRepository emergencyRepository) {
        this.contactRepository = contactRepository;
        this.emergencyRepository = emergencyRepository;
    }

    /**
     * Notify contacts for an emergency.
     * MOCK implementation: Logs the "SMS" and updates status.
     * Max 3 contacts notified.
     */
    @Transactional
    public void notifyContacts(Emergency emergency) {
        if (emergency.getContactNotificationStatus() != ContactNotificationStatus.PENDING) {
            log.warn("Skipping notification for emergency {}: Status is {}", emergency.getId(),
                    emergency.getContactNotificationStatus());
            return;
        }

        List<EmergencyContact> contacts = contactRepository.findByUserId(emergency.getUserId());

        if (contacts.isEmpty()) {
            log.info("No contacts found for user {}. Notification skipped.", emergency.getUserId());
            emergency.setContactNotificationStatus(ContactNotificationStatus.NOTIFIED); // Considered done
            emergencyRepository.save(emergency);
            return;
        }

        // Limit to 3 contacts
        int count = 0;
        for (EmergencyContact contact : contacts) {
            if (count >= 3)
                break;

            // MOCK SEND
            log.info(
                    "[MOCK SMS] To: {} ({}) - MSG: EMERGENCY ALERT! Your contact has reported an urgency. Location: {},{}",
                    contact.getName(), contact.getPhone(), emergency.getLatitude(), emergency.getLongitude());

            count++;
        }

        emergency.setContactNotificationStatus(ContactNotificationStatus.NOTIFIED);
        emergencyRepository.save(emergency);
        log.info("Emergency {} contacts notified (Mock Mode).", emergency.getId());
    }
}
