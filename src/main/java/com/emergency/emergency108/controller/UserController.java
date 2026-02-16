package com.emergency.emergency108.controller;

import com.emergency.emergency108.auth.security.AuthContext;
import com.emergency.emergency108.auth.guard.AuthGuard;
import com.emergency.emergency108.entity.User;
import com.emergency.emergency108.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserRepository userRepository;
    private final AuthGuard authGuard;

    public UserController(UserRepository userRepository, AuthGuard authGuard) {
        this.userRepository = userRepository;
        this.authGuard = authGuard;
    }

    /**
     * Update Helping Hand preference.
     * PUT /api/users/preferences/helping-hand
     * Body: { "enabled": true/false }
     */
    @PutMapping("/preferences/helping-hand")
    public ResponseEntity<?> updateHelpingHandPreference(@RequestBody Map<String, Boolean> request) {
        authGuard.requireAuthenticated();
        Long userId = AuthContext.getUserId();

        if (!request.containsKey("enabled")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing 'enabled' field"));
        }

        boolean enabled = request.get("enabled");

        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

            user.setHelpingHandEnabled(enabled);
            userRepository.save(user);

            log.info("User {} updated Helping Hand preference to: {}", userId, enabled);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", enabled ? "Helping Hand enabled" : "Helping Hand disabled",
                    "enabled", enabled));

        } catch (Exception e) {
            log.error("Failed to update preference for user {}: {}", userId, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal Error"));
        }
    }

    /**
     * Register or update FCM token for the authenticated user
     * POST /api/users/register-fcm-token
     * Body: { "fcmToken": "string" }
     */
    @PostMapping("/register-fcm-token")
    public ResponseEntity<?> registerFcmToken(@RequestBody Map<String, String> request) {
        authGuard.requireAuthenticated();
        Long userId = AuthContext.getUserId();

        String fcmToken = request.get("fcmToken");

        if (fcmToken == null || fcmToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "FCM token is required"));
        }

        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

            user.setFcmToken(fcmToken);
            user.setLastTokenUpdate(java.time.LocalDateTime.now());
            userRepository.save(user);

            log.info("FCM token registered for user {}: {}...",
                    userId,
                    fcmToken.substring(0, Math.min(20, fcmToken.length())));

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "FCM token registered successfully"));

        } catch (Exception e) {
            log.error("Failed to register FCM token for user {}: {}", userId, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal Error"));
        }
    }
}
