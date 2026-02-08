package com.emergency.emergency108.service;

import com.emergency.emergency108.entity.*;
import com.emergency.emergency108.repository.EmergencyAssignmentRepository;
import com.emergency.emergency108.repository.UserRepository;
import com.emergency.emergency108.util.DistanceCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for emergency-related authorization checks.
 * Ensures proper access control for emergency operations.
 */
@Service
public class EmergencyAuthorizationService {

    private final UserRepository userRepository;
    private final EmergencyAssignmentRepository assignmentRepository;
    private final DriverSessionService driverSessionService;

    public EmergencyAuthorizationService(
            UserRepository userRepository,
            EmergencyAssignmentRepository assignmentRepository,
            DriverSessionService driverSessionService) {
        this.userRepository = userRepository;
        this.assignmentRepository = assignmentRepository;
        this.driverSessionService = driverSessionService;
    }

    /**
     * Check if driver can accept emergencies.
     * Driver must be VERIFIED and currently ONLINE.
     * 
     * @param driverId Driver's user ID
     * @return true if driver can accept, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean canDriverAcceptEmergency(Long driverId) {
        Optional<User> userOpt = userRepository.findById(driverId);
        if (userOpt.isEmpty()) {
            return false;
        }

        User driver = userOpt.get();

        // Must be DRIVER role
        if (driver.getRole() != UserRole.DRIVER) {
            return false;
        }

        // Must be VERIFIED
        if (driver.getDriverVerificationStatus() != DriverVerificationStatus.VERIFIED) {
            return false;
        }

        // Must not be blocked
        if (driver.isBlocked() || !driver.isActive()) {
            return false;
        }

        // Must be currently ONLINE (not ON_TRIP)
        DriverSession session = driverSessionService.getActiveSession(driverId);
        if (session == null || session.getStatus() != DriverSessionStatus.ONLINE) {
            return false;
        }

        // Session must have recent heartbeat (< 1 hour for testing)
        if (session.getLastHeartbeat() != null) {
            LocalDateTime thirtySecondsAgo = LocalDateTime.now().minusSeconds(3600);
            if (session.getLastHeartbeat().isBefore(thirtySecondsAgo)) {
                return false; // Stale heartbeat
            }
        }

        return true;
    }

    /**
     * Check if driver is assigned to a specific emergency.
     * 
     * @param driverId Driver's user ID
     * @param emergencyId Emergency ID
     * @return true if driver is assigned, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isDriverAssignedToEmergency(Long driverId, Long emergencyId) {
        return assignmentRepository.findByEmergencyIdAndDriverIdAndStatus(
                emergencyId, driverId, EmergencyAssignmentStatus.ASSIGNED
        ).isPresent() || assignmentRepository.findByEmergencyIdAndDriverIdAndStatus(
                emergencyId, driverId, EmergencyAssignmentStatus.ACCEPTED
        ).isPresent();
    }

    /**
     * Check if user can cancel a specific emergency.
     * User must be the creator of the emergency.
     * 
     * @param userId User ID
     * @param emergency Emergency object
     * @return true if user can cancel, false otherwise
     */
    public boolean canUserCancelEmergency(Long userId, Emergency emergency) {
        // User must be the creator
        if (!emergency.getUserId().equals(userId)) {
            return false;
        }

        // Can only cancel if status is CREATED, DISPATCHED, IN_PROGRESS, AT_PATIENT, or TO_HOSPITAL
        EmergencyStatus status = emergency.getStatus();
        return status == EmergencyStatus.CREATED ||
               status == EmergencyStatus.DISPATCHED ||
               status == EmergencyStatus.IN_PROGRESS ||
               status == EmergencyStatus.AT_PATIENT ||
               status == EmergencyStatus.TO_HOSPITAL;
    }

    /**
     * Check if driver's current location is within 100 meters of hospital.
     * Used for emergency completion validation.
     * 
     * @param driverId Driver's user ID
     * @param hospitalLat Hospital latitude
     * @param hospitalLon Hospital longitude
     * @return Distance validation result
     */
    @Transactional(readOnly = true)
    public DistanceValidationResult isDriverWithin100Meters(Long driverId, Double hospitalLat, Double hospitalLon) {
        DriverSession session = driverSessionService.getActiveSession(driverId);
        
        if (session == null) {
            return new DistanceValidationResult(false, null, "Driver session not found");
        }

        if (session.getCurrentLat() == null || session.getCurrentLng() == null) {
            return new DistanceValidationResult(false, null, "Driver location not available");
        }

        double distance = DistanceCalculator.calculateDistance(
                session.getCurrentLat(), session.getCurrentLng(),
                hospitalLat, hospitalLon
        );

        boolean withinRange = distance <= 100.0;
        String message = withinRange 
                ? "Driver is within 100 meters of hospital" 
                : String.format("Driver is %.2f meters from hospital (must be within 100 meters)", distance);

        return new DistanceValidationResult(withinRange, distance, message);
    }

    /**
     * Check if driver is currently on a trip (has active assignment).
     * 
     * @param driverId Driver's user ID
     * @return true if driver is on trip, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isDriverOnTrip(Long driverId) {
        DriverSession session = driverSessionService.getActiveSession(driverId);
        return session != null && session.getStatus() == DriverSessionStatus.ON_TRIP;
    }

    /**
     * Result of distance validation check.
     */
    public static class DistanceValidationResult {
        private final boolean valid;
        private final Double distance;
        private final String message;

        public DistanceValidationResult(boolean valid, Double distance, String message) {
            this.valid = valid;
            this.distance = distance;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public Double getDistance() {
            return distance;
        }

        public String getMessage() {
            return message;
        }
    }
}
