package com.hackathon.emergency108.controller;

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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Get all pending driver verifications
     * GET /api/admin/pending-drivers
     */
    @GetMapping("/pending-drivers")
    public ResponseEntity<?> getPendingDrivers(@RequestHeader("Authorization") String authHeader) {
        try {
            // Validate admin token
            String token = authHeader.replace("Bearer ", "");
            AuthTokenPayload payload = tokenService.validateAndParse(token);

            User admin = userRepository.findById(payload.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (admin.getRole() != UserRole.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Only admins can view pending driver verifications");
            }

            // Fetch all pending drivers
            List<User> pendingDrivers = userRepository.findByRoleAndDriverVerificationStatus(
                    UserRole.DRIVER, DriverVerificationStatus.PENDING);

            // Map to response
            List<Map<String, Object>> driverList = pendingDrivers.stream()
                    .map(driver -> {
                        Map<String, Object> driverInfo = new HashMap<>();
                        driverInfo.put("id", driver.getId());
                        driverInfo.put("name", driver.getName());
                        driverInfo.put("phone", driver.getPhone());
                        driverInfo.put("email", driver.getEmail());
                        driverInfo.put("address", driver.getAddress());
                        driverInfo.put("documentUrl", driver.getDocumentUrl());
                        driverInfo.put("verificationStatus", driver.getDriverVerificationStatus());
                        driverInfo.put("createdAt", driver.getCreatedAt());
                        return driverInfo;
                    })
                    .collect(Collectors.toList());

            logger.info("✅ Admin {} fetched {} pending drivers", admin.getId(), driverList.size());

            Map<String, Object> response = new HashMap<>();
            response.put("totalPending", driverList.size());
            response.put("drivers", driverList);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            logger.error("❌ Error fetching pending drivers: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            logger.error("❌ Error fetching pending drivers: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch pending drivers");
        }
    }

    /**
     * Verify a driver (approve)
     * PUT /api/admin/verify-driver/{driverId}
     */
    @PutMapping("/verify-driver/{driverId}")
    public ResponseEntity<?> verifyDriver(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long driverId) {
        try {
            // Validate admin token
            String token = authHeader.replace("Bearer ", "");
            AuthTokenPayload payload = tokenService.validateAndParse(token);

            User admin = userRepository.findById(payload.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (admin.getRole() != UserRole.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Only admins can verify drivers");
            }

            // Get driver
            User driver = userRepository.findById(driverId)
                    .orElseThrow(() -> new RuntimeException("Driver not found with ID: " + driverId));

            if (driver.getRole() != UserRole.DRIVER) {
                return ResponseEntity.badRequest()
                        .body("User is not a driver");
            }

            // Mark as verified
            driver.setDriverVerificationStatus(DriverVerificationStatus.VERIFIED);
            userRepository.save(driver);

            logger.info("✅ Admin {} verified driver {} ({})", admin.getId(), driver.getId(), driver.getPhone());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Driver verified successfully");
            response.put("driverId", driver.getId());
            response.put("driverName", driver.getName());
            response.put("driverPhone", driver.getPhone());
            response.put("verificationStatus", driver.getDriverVerificationStatus());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            logger.error("❌ Error verifying driver: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            logger.error("❌ Error verifying driver: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to verify driver");
        }
    }

    /**
     * Reject a driver verification
     * PUT /api/admin/reject-driver/{driverId}
     */
    @PutMapping("/reject-driver/{driverId}")
    public ResponseEntity<?> rejectDriver(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long driverId) {
        try {
            // Validate admin token
            String token = authHeader.replace("Bearer ", "");
            AuthTokenPayload payload = tokenService.validateAndParse(token);

            User admin = userRepository.findById(payload.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (admin.getRole() != UserRole.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Only admins can reject drivers");
            }

            // Get driver
            User driver = userRepository.findById(driverId)
                    .orElseThrow(() -> new RuntimeException("Driver not found with ID: " + driverId));

            if (driver.getRole() != UserRole.DRIVER) {
                return ResponseEntity.badRequest()
                        .body("User is not a driver");
            }

            // Mark as rejected
            driver.setDriverVerificationStatus(DriverVerificationStatus.REJECTED);
            userRepository.save(driver);

            logger.info("✅ Admin {} rejected driver {} ({})", admin.getId(), driver.getId(), driver.getPhone());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Driver verification rejected");
            response.put("driverId", driver.getId());
            response.put("driverName", driver.getName());
            response.put("driverPhone", driver.getPhone());
            response.put("verificationStatus", driver.getDriverVerificationStatus());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            logger.error("❌ Error rejecting driver: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            logger.error("❌ Error rejecting driver: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to reject driver");
        }
    }
}
