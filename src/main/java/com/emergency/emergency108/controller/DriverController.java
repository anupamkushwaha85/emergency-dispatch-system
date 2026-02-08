package com.emergency.emergency108.controller;

import com.emergency.emergency108.auth.guard.AuthGuard;
import com.emergency.emergency108.auth.security.AuthContext;
import com.emergency.emergency108.dto.LocationUpdateRequest;
import com.emergency.emergency108.dto.StartShiftRequest;
import com.emergency.emergency108.entity.DriverSession;
import com.emergency.emergency108.entity.EmergencyAssignment;
import com.emergency.emergency108.entity.Hospital;
import com.emergency.emergency108.repository.EmergencyAssignmentRepository;
import com.emergency.emergency108.repository.HospitalRepository;
import com.emergency.emergency108.service.DriverSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Driver session management endpoints.
 * Handles driver shifts, location updates, and session queries.
 */
@RestController
@RequestMapping("/api/driver")
public class DriverController {

    private static final Logger log = LoggerFactory.getLogger(DriverController.class);

    private final DriverSessionService sessionService;
    private final AuthGuard authGuard;
    private final HospitalRepository hospitalRepository;
    private final EmergencyAssignmentRepository assignmentRepository;

    public DriverController(
            DriverSessionService sessionService,
            AuthGuard authGuard,
            HospitalRepository hospitalRepository,
            EmergencyAssignmentRepository assignmentRepository) {
        this.sessionService = sessionService;
        this.authGuard = authGuard;
        this.hospitalRepository = hospitalRepository;
        this.assignmentRepository = assignmentRepository;
    }

    /**
     * Start a new driver shift with specified ambulance.
     * 
     * POST /api/driver/start-shift
     * Body: { "ambulanceId": 5 }
     * 
     * Requirements:
     * - Driver must be VERIFIED
     * - No active session already exists
     * - Ambulance must be available
     */
    @PostMapping("/start-shift")
    public ResponseEntity<?> startShift(@RequestBody StartShiftRequest request) {
        authGuard.requireVerifiedDriver();

        Long driverId = AuthContext.get().getUserId();

        try {
            DriverSession session = sessionService.startShift(driverId, request.getAmbulanceId());

            return ResponseEntity.ok(Map.of(
                    "message", "Shift started successfully",
                    "sessionId", session.getId(),
                    "ambulanceId", session.getAmbulanceId(),
                    "status", session.getStatus(),
                    "startTime", session.getSessionStartTime()));

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    /**
     * End current driver shift.
     * 
     * POST /api/driver/end-shift
     * 
     * Requirements:
     * - Must have an active session
     * - Cannot be on an active trip
     */
    @PostMapping("/end-shift")
    public ResponseEntity<?> endShift() {
        authGuard.requireVerifiedDriver();

        Long driverId = AuthContext.get().getUserId();

        try {
            sessionService.endShift(driverId);

            return ResponseEntity.ok(Map.of(
                    "message", "Shift ended successfully"));

        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    /**
     * Get current active session.
     * 
     * GET /api/driver/current-session
     * 
     * Returns session details or 404 if no active session.
     */
    @GetMapping("/current-session")
    public ResponseEntity<?> getCurrentSession() {
        authGuard.requireVerifiedDriver();

        Long driverId = AuthContext.get().getUserId();

        return sessionService.getCurrentSession(driverId)
                .map(session -> ResponseEntity.ok(Map.of(
                        "sessionId", session.getId(),
                        "ambulanceId", session.getAmbulanceId(),
                        "status", session.getStatus(),
                        "startTime", session.getSessionStartTime(),
                        "currentLat", session.getCurrentLat() != null ? session.getCurrentLat() : 0.0,
                        "currentLng", session.getCurrentLng() != null ? session.getCurrentLng() : 0.0,
                        "locationUpdatedAt", session.getLocationUpdatedAt(),
                        "emergenciesHandled", session.getEmergenciesHandled())))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No active session found. Start a shift first."));
    }

    /**
     * Update driver's current location.
     * 
     * PUT /api/driver/location
     * Body: { "lat": 28.6139, "lng": 77.209 }
     * 
     * HEARTBEAT MECHANISM:
     * - Driver app must call this endpoint every 3-5 seconds automatically
     * - Updates both GPS location AND heartbeat timestamp
     * - If heartbeat not received for 30+ seconds, driver auto-marked OFFLINE
     * - This prevents crashed/disconnected drivers from receiving assignments
     */
    @PutMapping("/location")
    public ResponseEntity<?> updateLocation(@RequestBody LocationUpdateRequest request) {
        authGuard.requireVerifiedDriver();

        Long driverId = AuthContext.get().getUserId();

        // Validate coordinates
        if (!isValidCoordinate(request.getLat(), request.getLng())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid coordinates. Lat must be -90 to 90, Lng must be -180 to 180");
        }

        try {
            sessionService.updateLocation(driverId, request.getLat(), request.getLng());

            return ResponseEntity.ok(Map.of(
                    "message", "Location and heartbeat updated",
                    "lat", request.getLat(),
                    "lng", request.getLng(),
                    "timestamp", LocalDateTime.now()));

        } catch (IllegalStateException e) {
            log.warn("Failed to update location for driver {}: {}", driverId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error updating location for driver {}: {}", driverId, e.getMessage(), e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to update location. Please try again.");
        }
    }

    /**
     * Validate geographic coordinates.
     */
    private boolean isValidCoordinate(double lat, double lng) {
        return lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180;
    }

    /**
     * Get driver's session history.
     * 
     * GET /api/driver/history
     * 
     * Returns all past and current sessions for analytics.
     */
    @GetMapping("/history")
    public ResponseEntity<List<DriverSession>> getHistory() {
        authGuard.requireVerifiedDriver();

        Long driverId = AuthContext.get().getUserId();

        List<DriverSession> history = sessionService.getDriverHistory(driverId);

        return ResponseEntity.ok(history);
    }

    /**
     * Mark patient as picked up.
     * Automatically assigns the nearest hospital as destination.
     * 
     * POST /api/driver/mark-patient-picked-up
     * Body: { "emergencyId": 5, "patientLat": 28.6139, "patientLng": 77.209 }
     */
    @PostMapping("/mark-patient-picked-up")
    public ResponseEntity<?> markPatientPickedUp(@RequestBody Map<String, Object> request) {
        authGuard.requireVerifiedDriver();

        try {
            Long emergencyId = Long.valueOf(request.get("emergencyId").toString());
            Double patientLat = Double.valueOf(request.get("patientLat").toString());
            Double patientLng = Double.valueOf(request.get("patientLng").toString());

            // Find assignment
            EmergencyAssignment assignment = assignmentRepository.findTopByEmergencyIdOrderByAssignedAtDesc(emergencyId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Assignment not found"));

            // Find nearest hospital
            List<Hospital> nearestHospitals = hospitalRepository.findNearestHospitals(
                    patientLat,
                    patientLng,
                    1);

            if (nearestHospitals.isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "No hospitals found in database");
            }

            Hospital nearestHospital = nearestHospitals.get(0);
            assignment.setDestinationHospital(nearestHospital);
            assignmentRepository.save(assignment);

            return ResponseEntity.ok(Map.of(
                    "message", "Patient picked up, heading to hospital",
                    "hospital", Map.of(
                            "id", nearestHospital.getId(),
                            "name", nearestHospital.getName(),
                            "latitude", nearestHospital.getLatitude(),
                            "longitude", nearestHospital.getLongitude(),
                            "address", nearestHospital.getAddress())));

        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid coordinates");
        }
    }

    /**
     * Complete mission (drop patient at hospital).
     * Validates driver is within 100 meters of assigned hospital.
     * 
     * POST /api/driver/complete-mission
     * Body: { "emergencyId": 5, "currentLat": 28.5672, "currentLng": 77.2100 }
     */
    @PostMapping("/complete-mission")
    public ResponseEntity<?> completeMission(@RequestBody Map<String, Object> request) {
        authGuard.requireVerifiedDriver();

        try {
            Long emergencyId = Long.valueOf(request.get("emergencyId").toString());
            Double currentLat = Double.valueOf(request.get("currentLat").toString());
            Double currentLng = Double.valueOf(request.get("currentLng").toString());

            // Find assignment
            EmergencyAssignment assignment = assignmentRepository.findTopByEmergencyIdOrderByAssignedAtDesc(emergencyId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Assignment not found"));

            Hospital hospital = assignment.getDestinationHospital();
            if (hospital == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "No hospital assigned. Pick up patient first.");
            }

            // Calculate distance using Haversine formula
            double distance = calculateDistance(
                    currentLat,
                    currentLng,
                    hospital.getLatitude(),
                    hospital.getLongitude());

            if (distance > 0.1) { // 0.1 km = 100 meters
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "error", "Too far from hospital",
                        "message", String.format("You must be within 100m of %s. Current distance: %.0fm",
                                hospital.getName(), distance * 1000),
                        "distanceKm", distance,
                        "requiredDistanceKm", 0.1));
            }

            // Complete mission
            assignment.setCompletedAt(LocalDateTime.now());
            assignmentRepository.save(assignment);

            return ResponseEntity.ok(Map.of(
                    "message", "Mission completed successfully",
                    "hospital", hospital.getName(),
                    "distanceKm", distance));

        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid coordinates");
        }
    }

    /**
     * Calculate distance between two points using Haversine formula.
     * Returns distance in kilometers.
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the Earth in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * Check if driver is currently online.
     * 
     * GET /api/driver/status
     */
    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        authGuard.requireVerifiedDriver();

        Long driverId = AuthContext.get().getUserId();

        boolean isOnline = sessionService.isDriverOnline(driverId);

        return ResponseEntity.ok(Map.of(
                "driverId", driverId,
                "isOnline", isOnline));
    }
}
