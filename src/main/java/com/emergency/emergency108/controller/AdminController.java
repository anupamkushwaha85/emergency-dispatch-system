package com.emergency.emergency108.controller;

import com.emergency.emergency108.auth.token.AuthTokenPayload;
import com.emergency.emergency108.auth.token.TokenService;
import com.emergency.emergency108.entity.DriverVerificationStatus;
import com.emergency.emergency108.entity.Emergency;
import com.emergency.emergency108.entity.EmergencyStatus;
import com.emergency.emergency108.entity.User;
import com.emergency.emergency108.entity.UserRole;
import com.emergency.emergency108.repository.EmergencyRepository;
import com.emergency.emergency108.repository.UserRepository;
import com.emergency.emergency108.repository.AmbulanceRepository;
import com.emergency.emergency108.entity.Ambulance;
import com.emergency.emergency108.entity.AmbulanceStatus;
import com.emergency.emergency108.entity.DriverSession;
import com.emergency.emergency108.service.DriverSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final EmergencyRepository emergencyRepository;
    private final AmbulanceRepository ambulanceRepository;
    private final DriverSessionService driverSessionService;

    public AdminController(TokenService tokenService,
            UserRepository userRepository,
            EmergencyRepository emergencyRepository,
            AmbulanceRepository ambulanceRepository,
            DriverSessionService driverSessionService) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
        this.emergencyRepository = emergencyRepository;
        this.ambulanceRepository = ambulanceRepository;
        this.driverSessionService = driverSessionService;
    }

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

    /**
     * Get all verified drivers for assignment
     * GET /api/admin/verified-drivers
     */
    @GetMapping("/verified-drivers")
    public ResponseEntity<?> getVerifiedDrivers(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            AuthTokenPayload payload = tokenService.validateAndParse(token);

            User admin = userRepository.findById(payload.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (admin.getRole() != UserRole.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Only admins can view verified drivers");
            }

            List<User> verifiedDrivers = userRepository.findByRoleAndDriverVerificationStatus(
                    UserRole.DRIVER, DriverVerificationStatus.VERIFIED);

            List<Map<String, Object>> drivers = verifiedDrivers.stream()
                    .map(d -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", d.getId());
                        map.put("name", d.getName());
                        map.put("phone", d.getPhone());
                        map.put("status", d.getDriverVerificationStatus());
                        return map;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(drivers);

        } catch (RuntimeException e) {
            logger.error("❌ Error fetching verified drivers: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            logger.error("❌ Error fetching verified drivers: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch verified drivers");
        }
    }

    /**
     * Get all currently online/active drivers
     * GET /api/admin/online-drivers
     */
    @GetMapping("/online-drivers")
    public ResponseEntity<?> getOnlineDrivers(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            AuthTokenPayload payload = tokenService.validateAndParse(token);

            User admin = userRepository.findById(payload.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (admin.getRole() != UserRole.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Only admins can view online drivers");
            }

            List<DriverSession> onlineSessions = driverSessionService.getAllOnlineDrivers();

            List<Long> driverIds = onlineSessions.stream()
                    .map(DriverSession::getDriverId)
                    .distinct()
                    .collect(Collectors.toList());
            List<User> driversList = userRepository.findAllById(driverIds);
            Map<Long, User> driverMap = driversList.stream()
                    .collect(Collectors.toMap(User::getId, d -> d));

            List<Long> ambulanceIds = onlineSessions.stream()
                    .map(DriverSession::getAmbulanceId)
                    .distinct()
                    .collect(Collectors.toList());
            List<Ambulance> ambulancesList = ambulanceRepository.findAllById(ambulanceIds);
            Map<Long, Ambulance> ambulanceMap = ambulancesList.stream()
                    .collect(Collectors.toMap(Ambulance::getId, a -> a));

            List<Map<String, Object>> response = onlineSessions.stream()
                    .map(session -> {
                        Map<String, Object> sessionData = new HashMap<>();
                        sessionData.put("sessionId", session.getId());
                        sessionData.put("driverId", session.getDriverId());
                        sessionData.put("ambulanceId", session.getAmbulanceId());
                        sessionData.put("status", session.getStatus());
                        sessionData.put("latitude", session.getCurrentLat());
                        sessionData.put("longitude", session.getCurrentLng());
                        sessionData.put("sessionStartTime", session.getSessionStartTime());

                        // Populate driver info
                        User driver = driverMap.get(session.getDriverId());
                        if (driver != null) {
                            sessionData.put("driverName", driver.getName());
                            sessionData.put("driverPhone", driver.getPhone());
                        }

                        // Populate ambulance info
                        Ambulance amb = ambulanceMap.get(session.getAmbulanceId());
                        if (amb != null) {
                            sessionData.put("licensePlate", amb.getLicensePlate());
                        }

                        return sessionData;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            logger.error("❌ Error fetching online drivers: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            logger.error("❌ Error fetching online drivers: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch online drivers");
        }
    }

    /**
     * Get Dashboard Stats
     * GET /api/admin/dashboard-stats
     */
    @GetMapping("/dashboard-stats")
    public ResponseEntity<?> getDashboardStats(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            AuthTokenPayload payload = tokenService.validateAndParse(token);

            User admin = userRepository.findById(payload.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (admin.getRole() != UserRole.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Only admins can view dashboard stats");
            }

            // 1. Active Emergencies (Not COMPLETED or CANCELLED)
            java.util.List<EmergencyStatus> inactiveStatuses = java.util.Arrays.asList(
                    EmergencyStatus.COMPLETED, EmergencyStatus.CANCELLED);
            long activeEmergenciesCount = emergencyRepository.countByStatusNotIn(inactiveStatuses);

            // 2. Available Ambulances
            long availableAmbulancesCount = ambulanceRepository.findByStatus(AmbulanceStatus.AVAILABLE).size();

            // 3. Pending Drivers
            List<User> pendingDrivers = userRepository.findByRoleAndDriverVerificationStatus(
                    UserRole.DRIVER, DriverVerificationStatus.PENDING);
            int pendingDriversCount = pendingDrivers.size();

            Map<String, Object> response = new HashMap<>();
            response.put("activeEmergencies", activeEmergenciesCount);
            response.put("availableAmbulances", availableAmbulancesCount);
            response.put("pendingDrivers", pendingDriversCount);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            logger.error("❌ Error fetching dashboard stats: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            logger.error("❌ Error fetching dashboard stats: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch dashboard stats");
        }
    }

    /**
     * Get Active Emergencies for Live Map
     * GET /api/admin/active-emergencies
     */
    @GetMapping("/active-emergencies")
    public ResponseEntity<?> getActiveEmergencies(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            AuthTokenPayload payload = tokenService.validateAndParse(token);

            // Validate admin (optional, assuming map is admin only)
            userRepository.findById(payload.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            java.util.List<EmergencyStatus> inactiveStatuses = java.util.Arrays.asList(
                    EmergencyStatus.COMPLETED, EmergencyStatus.CANCELLED);
            List<Emergency> active = emergencyRepository.findByStatusNotIn(inactiveStatuses);

            // Note: Enum has CREATED, IN_PROGRESS, DISPATCHED, AT_PATIENT, TO_HOSPITAL,
            // COMPLETED, CANCELLED, UNASSIGNED
            // We want all except COMPLETED and CANCELLED.

            return ResponseEntity.ok(active);

        } catch (RuntimeException e) {
            logger.error("❌ Error fetching active emergencies: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            logger.error("❌ Error fetching active emergencies: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch active emergencies");
        }
    }
}
