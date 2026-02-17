package com.emergency.emergency108.controller;

import com.emergency.emergency108.entity.User;
import com.emergency.emergency108.entity.UserRole;
import com.emergency.emergency108.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/setup")
public class SetupController {

    private static final Logger logger = LoggerFactory.getLogger(SetupController.class);

    @Autowired
    private UserRepository userRepository;

    /**
     * Secret endpoint to promote a user to ADMIN.
     * Use cautiously!
     * GET /api/setup/promote-admin?phone={phoneNumber}
     */
    @GetMapping("/promote-admin")
    public ResponseEntity<?> promoteAdmin(@RequestParam String phone) {
        try {
            Optional<User> userOpt = userRepository.findByPhone(phone);

            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("User not found with phone: " + phone);
            }

            User user = userOpt.get();

            // If already admin
            if (user.getRole() == UserRole.ADMIN) {
                return ResponseEntity.ok(Map.of(
                        "message", "User is already an ADMIN",
                        "phone", user.getPhone()));
            }

            user.setRole(UserRole.ADMIN);
            userRepository.save(user);

            logger.warn("🚨 SECURITY ALERT: User {} promoted to ADMIN via secret endpoint", phone);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "User promoted to ADMIN successfully",
                    "phone", user.getPhone(),
                    "role", "ADMIN"));

        } catch (Exception e) {
            logger.error("Error promoting admin: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to promote user");
        }
    }
}
