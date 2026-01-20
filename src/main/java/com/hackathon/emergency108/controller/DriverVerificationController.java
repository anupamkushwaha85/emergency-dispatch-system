package com.hackathon.emergency108.controller;

import com.hackathon.emergency108.auth.dto.DriverVerificationRequest;
import com.hackathon.emergency108.auth.token.AuthTokenPayload;
import com.hackathon.emergency108.auth.token.TokenService;
import com.hackathon.emergency108.entity.DriverVerificationStatus;
import com.hackathon.emergency108.entity.User;
import com.hackathon.emergency108.entity.UserRole;
import com.hackathon.emergency108.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/driver")
public class DriverVerificationController {

    private static final Logger logger = LoggerFactory.getLogger(DriverVerificationController.class);

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Driver requests verification by uploading documents
     * POST /api/driver/request-verification
     */
    @PostMapping("/request-verification")
    public ResponseEntity<?> requestVerification(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody DriverVerificationRequest request) {
        try {
            // Extract and validate token
            String token = authHeader.replace("Bearer ", "");
            AuthTokenPayload payload = tokenService.validateAndParse(token);

            // Get user
            User driver = userRepository.findById(payload.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Validate user is a driver
            if (driver.getRole() != UserRole.DRIVER) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Only drivers can request verification");
            }

            // Validate document URL
            if (request.getDocumentUrl() == null || request.getDocumentUrl().isEmpty()) {
                return ResponseEntity.badRequest().body("Document URL is required");
            }

            // Update driver verification details
            driver.setDocumentUrl(request.getDocumentUrl());
            driver.setDriverVerificationStatus(DriverVerificationStatus.PENDING);

            userRepository.save(driver);

            logger.info("✅ Driver {} submitted verification request with document: {}",
                    driver.getId(), request.getDocumentUrl());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Verification request submitted successfully");
            response.put("status", "PENDING");
            response.put("driverId", driver.getId());
            response.put("documentUrl", driver.getDocumentUrl());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            logger.error("❌ Verification request failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            logger.error("❌ Error processing verification request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to process verification request: " + e.getMessage());
        }
    }

    /**
     * Get driver verification status
     * GET /api/driver/verification-status
     */
    @GetMapping("/verification-status")
    public ResponseEntity<?> getVerificationStatus(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            AuthTokenPayload payload = tokenService.validateAndParse(token);

            User driver = userRepository.findById(payload.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (driver.getRole() != UserRole.DRIVER) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Only drivers can check verification status");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("driverId", driver.getId());
            response.put("verificationStatus", driver.getDriverVerificationStatus());
            response.put("documentUrl", driver.getDocumentUrl());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ Error fetching verification status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch verification status");
        }
    }
}
