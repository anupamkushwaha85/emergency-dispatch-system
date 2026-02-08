package com.emergency.emergency108.auth.controller;

import com.emergency.emergency108.auth.dto.*;
import com.emergency.emergency108.auth.service.OtpService;
import com.emergency.emergency108.auth.token.AuthTokenPayload;
import com.emergency.emergency108.auth.token.TokenService;
import com.emergency.emergency108.entity.User;
import com.emergency.emergency108.entity.UserRole;
import com.emergency.emergency108.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class OtpAuthController {

    private static final Logger logger = LoggerFactory.getLogger(OtpAuthController.class);

    @Autowired
    private OtpService otpService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserRepository userRepository;

    /**
     * STEP 1: Send OTP to phone number
     * POST /api/auth/send-otp
     */
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody SendOtpRequest request) {
        try {
            logger.info("📱 OTP request received for phone: {} with role: {}", request.getPhone(), request.getRole());

            // Validate input
            if (request.getPhone() == null || request.getPhone().isEmpty()) {
                return ResponseEntity.badRequest().body("Phone number is required");
            }

            // Default to PUBLIC if no role specified
            UserRole role = request.getRole() != null ? request.getRole() : UserRole.PUBLIC;

            // Generate and send OTP
            String otp = otpService.sendOtp(request.getPhone(), role);

            // Return response (include OTP only for testing - remove in production)
            OtpResponse response = new OtpResponse(
                    "OTP sent successfully to " + request.getPhone(),
                    request.getPhone(),
                    otp // ⚠️ Remove this in production!
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ Error sending OTP: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to send OTP: " + e.getMessage());
        }
    }

    /**
     * STEP 2: Verify OTP and get JWT token
     * POST /api/auth/verify-otp
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody VerifyOtpRequest request) {
        try {
            logger.info("🔐 OTP verification requested for phone: {}", request.getPhone());

            // Validate input
            if (request.getPhone() == null || request.getPhone().isEmpty()) {
                return ResponseEntity.badRequest().body("Phone number is required");
            }
            if (request.getOtp() == null || request.getOtp().isEmpty()) {
                return ResponseEntity.badRequest().body("OTP is required");
            }

            // Verify OTP (includes admin passkey check if user is ADMIN)
            User user = otpService.verifyOtp(request.getPhone(), request.getOtp(), request.getAdminPasskey());

            // Generate JWT token
            AuthTokenPayload payload = new AuthTokenPayload(user.getId(), user.getRole());
            String token = tokenService.generate(payload);

            // Return token and user details
            AuthResponse response = new AuthResponse(token, user);

            logger.info("✅ OTP verified successfully for user: {} with role: {}", user.getId(), user.getRole());
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            logger.error("❌ OTP verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            logger.error("❌ Error verifying OTP: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to verify OTP: " + e.getMessage());
        }
    }

    /**
     * STEP 3: Update user profile
     * PUT /api/auth/profile
     */
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody ProfileUpdateRequest request) {
        try {
            // Extract token
            String token = authHeader.replace("Bearer ", "");
            AuthTokenPayload payload = tokenService.validateAndParse(token);

            // Get user
            User user = userRepository.findById(payload.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Update profile fields
            if (request.getName() != null && !request.getName().isEmpty()) {
                user.setName(request.getName());
            }
            if (request.getAddress() != null && !request.getAddress().isEmpty()) {
                user.setAddress(request.getAddress());
            }
            if (request.getEmail() != null && !request.getEmail().isEmpty()) {
                user.setEmail(request.getEmail());
            }
            if (request.getLanguage() != null && !request.getLanguage().isEmpty()) {
                user.setLanguage(request.getLanguage());
            }
            if (request.getGender() != null && !request.getGender().isEmpty()) {
                user.setGender(request.getGender());
            }
            if (request.getDateOfBirth() != null && !request.getDateOfBirth().isEmpty()) {
                try {
                    java.time.LocalDate dob = java.time.LocalDate.parse(request.getDateOfBirth());
                    user.setDateOfBirth(dob);

                    // Calculate age from DOB
                    int age = java.time.Period.between(dob, java.time.LocalDate.now()).getYears();
                    user.setAge(age);
                    logger.info("📅 DOB set and age calculated: {} years", age);
                } catch (Exception e) {
                    logger.error("❌ Invalid date format for DOB: {}", request.getDateOfBirth());
                }
            }
            // Blood group is optional
            if (request.getBloodGroup() != null && !request.getBloodGroup().isEmpty()
                    && !request.getBloodGroup().equals("Select")) {
                user.setBloodGroup(request.getBloodGroup());
            }

            // Mark profile as complete if all required fields are filled
            if (user.getName() != null && !user.getName().startsWith("User ") &&
                    user.getAddress() != null && !user.getAddress().isEmpty() &&
                    user.getGender() != null && !user.getGender().isEmpty() &&
                    user.getDateOfBirth() != null) {
                user.setProfileComplete(true);
            }

            userRepository.save(user);

            logger.info("✅ Profile updated successfully for user: {}", user.getId());

            // Return updated user details
            AuthResponse response = new AuthResponse(token, user);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            logger.error("❌ Profile update failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            logger.error("❌ Error updating profile: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to update profile: " + e.getMessage());
        }
    }
}
