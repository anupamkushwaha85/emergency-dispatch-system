package com.emergency.emergency108.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Service for sending Firebase Cloud Messaging push notifications
 */
@Service
public class FCMNotificationService {

    private static final Logger log = LoggerFactory.getLogger(FCMNotificationService.class);

    /**
     * Send a push notification to a single device
     */
    public boolean sendPushNotification(String fcmToken, String title, String body, Map<String, String> data) {
        if (fcmToken == null || fcmToken.isBlank()) {
            log.warn("Cannot send notification: FCM token is null or empty");
            return false;
        }

        try {
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            Message.Builder messageBuilder = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(notification);

            // Add custom data if provided
            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }

            Message message = messageBuilder.build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("Successfully sent FCM notification to token {}: {}",
                    fcmToken.substring(0, Math.min(20, fcmToken.length())) + "...",
                    response);
            return true;

        } catch (FirebaseMessagingException e) {
            log.error("Failed to send FCM notification to token {}: {}",
                    fcmToken.substring(0, Math.min(20, fcmToken.length())) + "...",
                    e.getMessage());
            return false;
        }
    }

    /**
     * Send push notifications to multiple devices
     */
    public int sendBatchNotifications(List<String> fcmTokens, String title, String body, Map<String, String> data) {
        if (fcmTokens == null || fcmTokens.isEmpty()) {
            log.warn("Cannot send batch notifications: token list is null or empty");
            return 0;
        }

        int successCount = 0;
        for (String token : fcmTokens) {
            if (sendPushNotification(token, title, body, data)) {
                successCount++;
            }
        }

        log.info("Sent {} out of {} batch FCM notifications successfully", successCount, fcmTokens.size());
        return successCount;
    }
}
