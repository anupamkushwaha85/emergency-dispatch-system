package com.emergency.emergency108.controller;

import com.emergency.emergency108.auth.security.AuthContext;
import com.emergency.emergency108.entity.EmergencyAssignment;
import com.emergency.emergency108.service.EmergencyAssignmentService;
import com.emergency.emergency108.service.EmergencyAuthorizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for driver emergency operations.
 * Handles accept, reject, and viewing assigned emergencies.
 * All endpoints require VERIFIED + ONLINE driver authentication.
 */
@RestController
@RequestMapping("/api/driver/emergencies")
public class DriverEmergencyController {

    private final EmergencyAssignmentService assignmentService;
    private final EmergencyAuthorizationService authorizationService;

    public DriverEmergencyController(
            EmergencyAssignmentService assignmentService,
            EmergencyAuthorizationService authorizationService) {
        this.assignmentService = assignmentService;
        this.authorizationService = authorizationService;
    }

    /**
     * Driver accepts an emergency assignment.
     * Authorization: Only VERIFIED + ONLINE driver who is assigned can accept.
     * 
     * POST /api/driver/emergencies/{emergencyId}/accept
     */
    @PostMapping("/{emergencyId}/accept")
    public ResponseEntity<?> acceptEmergency(@PathVariable Long emergencyId) {
        Long driverId = AuthContext.getUserId();

        // Authorization: Check if driver can accept emergencies
        if (!authorizationService.canDriverAcceptEmergency(driverId)) {
            return ResponseEntity.status(403).body(Map.of(
                    "error", "Unauthorized",
                    "message", "Only VERIFIED and ONLINE drivers can accept emergencies"
            ));
        }

        // Authorization: Check if driver is assigned to this emergency
        if (!authorizationService.isDriverAssignedToEmergency(driverId, emergencyId)) {
            return ResponseEntity.status(403).body(Map.of(
                    "error", "Unauthorized",
                    "message", "You are not assigned to this emergency"
            ));
        }

        try {
            EmergencyAssignment assignment = assignmentService.acceptEmergency(emergencyId, driverId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Emergency accepted successfully",
                    "assignment", assignment,
                    "emergencyId", emergencyId
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid State",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Internal Error",
                    "message", "Failed to accept emergency: " + e.getMessage()
            ));
        }
    }

    /**
     * Driver rejects an emergency assignment.
     * Authorization: Only VERIFIED driver who is assigned can reject.
     * 
     * POST /api/driver/emergencies/{emergencyId}/reject
     */
    @PostMapping("/{emergencyId}/reject")
    public ResponseEntity<?> rejectEmergency(@PathVariable Long emergencyId) {
        Long driverId = AuthContext.getUserId();

        // Authorization: Check if driver is assigned to this emergency
        if (!authorizationService.isDriverAssignedToEmergency(driverId, emergencyId)) {
            return ResponseEntity.status(403).body(Map.of(
                    "error", "Unauthorized",
                    "message", "You are not assigned to this emergency"
            ));
        }

        try {
            assignmentService.rejectEmergency(emergencyId, driverId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Emergency rejected, finding next available driver"
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid State",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Internal Error",
                    "message", "Failed to reject emergency: " + e.getMessage()
            ));
        }
    }

    /**
     * Get currently assigned emergency for driver.
     * Authorization: Only VERIFIED driver.
     * 
     * GET /api/driver/emergencies/assigned
     */
    @GetMapping("/assigned")
    public ResponseEntity<?> getAssignedEmergency() {
        Long driverId = AuthContext.getUserId();

        try {
            EmergencyAssignment assignment = assignmentService.getCurrentAssignment(driverId);
            
            if (assignment == null) {
                return ResponseEntity.ok(Map.of(
                        "assigned", false,
                        "message", "No emergency currently assigned"
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "assigned", true,
                    "assignment", assignment,
                    "emergency", assignment.getEmergency(),
                    "status", assignment.getStatus(),
                    "assignedAt", assignment.getAssignedAt(),
                    "responseDeadline", assignment.getResponseDeadline()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Internal Error",
                    "message", "Failed to get assigned emergency: " + e.getMessage()
            ));
        }
    }
}
