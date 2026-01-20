package com.hackathon.emergency108.controller;


import com.hackathon.emergency108.auth.guard.AuthGuard;
import com.hackathon.emergency108.auth.security.AuthContext;
import com.hackathon.emergency108.dto.EmergencyTimelineEvent;
import com.hackathon.emergency108.entity.*;
import com.hackathon.emergency108.metrics.DomainMetrics;
import com.hackathon.emergency108.repository.AmbulanceRepository;
import com.hackathon.emergency108.repository.EmergencyAssignmentRepository;
import com.hackathon.emergency108.service.DriverSessionService;
import com.hackathon.emergency108.service.EmergencyAssignmentService;

import com.hackathon.emergency108.repository.EmergencyRepository;

import com.hackathon.emergency108.service.EmergencyDispatchService;
import com.hackathon.emergency108.service.EmergencyTimelineService;
import com.hackathon.emergency108.system.SystemReadiness;
import com.hackathon.emergency108.util.GeoUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.micrometer.core.instrument.Timer;

@RestController
@RequestMapping("/api/emergencies")
public class EmergencyController {

    private static final Logger log = LoggerFactory.getLogger(EmergencyController.class);

    private final EmergencyRepository emergencyRepository;
    private final EmergencyDispatchService emergencyDispatchService;
    private final EmergencyAssignmentRepository assignmentRepository;
    private final DriverSessionService driverSessionService;
    private final DomainMetrics metrics;
    private final AuthGuard authGuard;
    private final SystemReadiness systemReadiness;
    private final EmergencyTimelineService emergencyTimelineService;
    private final EmergencyAssignmentService assignmentService;
    private final AmbulanceRepository ambulanceRepository;



    public EmergencyController(EmergencyRepository emergencyRepository,
                               EmergencyDispatchService emergencyDispatchService,
                               AmbulanceRepository ambulanceRepository,
                               EmergencyAssignmentService assignmentService,
                               EmergencyTimelineService emergencyTimelineService,
                               EmergencyAssignmentRepository assignmentRepository,
                               DriverSessionService driverSessionService,
                               SystemReadiness systemReadiness,
                               DomainMetrics metrics,
                               AuthGuard authGuard) {
        this.authGuard = authGuard;
        this.metrics = metrics;
        this.systemReadiness = systemReadiness;
        this.emergencyTimelineService = emergencyTimelineService;
        this.emergencyRepository = emergencyRepository;
        this.emergencyDispatchService = emergencyDispatchService;
        this.ambulanceRepository = ambulanceRepository;
        this.assignmentService = assignmentService;
        this.assignmentRepository = assignmentRepository;
        this.driverSessionService = driverSessionService;
    }

    @PostMapping
    public Emergency createEmergency(@RequestBody Emergency emergency) {

        authGuard.requireAuthenticated(); // 🔐 REQUIRED

        emergency.setStatus(EmergencyStatus.CREATED);
        return emergencyRepository.save(emergency);
    }

    @Autowired
    private EntityManager entityManager;

    @Transactional
    @PostMapping("/{id}/dispatch")
    public ResponseEntity<Ambulance> dispatch(@PathVariable Long id) {

        authGuard.requireAuthenticated();
        metrics.dispatchAttempt();
        Timer.Sample sample = metrics.startDispatchTimer();

        try {

        if (!systemReadiness.isReady()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "System is recovering. Please retry shortly."
            );
        }

        Emergency emergency = entityManager.find(Emergency.class, id);

        if (emergency == null) {
            throw new RuntimeException("Emergency not found");
        }

        // 🔒 PESSIMISTIC LOCK (MariaDB safe)
        entityManager.lock(emergency, LockModeType.PESSIMISTIC_WRITE);

        // ✅ STEP 1: Ensure correct emergency state
        if (emergency.getStatus() == EmergencyStatus.CREATED) {
            assignmentService.markEmergencyInProgress(emergency);
        }

        // ❌ Guard against double dispatch
        if (assignmentService.isAlreadyAssigned(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Emergency already assigned"
            );
        }

        // 🚑 STEP 2: Assign ambulance
        Ambulance ambulance = emergencyDispatchService.assignNearestAmbulance(
                emergency.getLatitude(),
                emergency.getLongitude()
        );

        // 📌 STEP 3: Create assignment
        assignmentService.assign(emergency, ambulance);

            metrics.dispatchSuccess();

        return ResponseEntity.ok(ambulance);
        } finally {
            metrics.stopDispatchTimer(sample);
        }
    }




    @PostMapping("/{id}/reject/{ambulanceId}")
    public ResponseEntity<Ambulance> rejectAndRetry(
            @PathVariable Long id,
            @PathVariable Long ambulanceId
    ) {

        authGuard.requireVerifiedDriver();
        if (!systemReadiness.isReady()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "System recovering, retry shortly"
            );
        }
        Emergency emergency = emergencyRepository.findById(id)
                .orElseThrow();

        Ambulance ambulance = ambulanceRepository.findById(ambulanceId)
                .orElseThrow();

        Ambulance next = assignmentService
                .rejectAndRetry(emergency, ambulance);

        return ResponseEntity.ok(next);
    }


    @GetMapping("/{id}/timeline")
    public List<EmergencyTimelineEvent> timeline(@PathVariable Long id) {
        authGuard.requireAuthenticated();
        return emergencyTimelineService.getTimeline(id);
    }

    @PostMapping("/{id}/respond")
    public ResponseEntity<Void> respondToAssignment(
            @PathVariable Long id,
            @RequestParam boolean accepted
    ) {
        if (!systemReadiness.isReady()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "System recovering, retry shortly"
            );
        }

        authGuard.requireVerifiedDriver();

        assignmentService.respondToAssignment(id, accepted);
        return ResponseEntity.ok().build();
    }

    /**
     * Driver reports arrival at patient location.
     * 
     * POST /api/emergencies/{id}/arrive
     * 
     * Transitions: DISPATCHED → AT_PATIENT
     * 
     * AUTHORIZATION: Must be the assigned driver
     */
    @PostMapping("/{id}/arrive")
    public ResponseEntity<Map<String, Object>> markArrivalAtPatient(@PathVariable Long id) {
        authGuard.requireVerifiedDriver();
        
        Long driverId = AuthContext.get().getUserId();
        
        try {
            Emergency emergency = emergencyRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, 
                        "Emergency not found: " + id
                    ));
            
            // Verify driver is assigned to this emergency
            List<EmergencyAssignment> assignments = assignmentRepository.findByEmergencyId(id);
            boolean isAssignedDriver = assignments.stream()
                    .anyMatch(a -> driverId.equals(a.getDriverId()) 
                                && a.getStatus() == EmergencyAssignmentStatus.ACCEPTED);
            
            if (!isAssignedDriver) {
                throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not assigned to this emergency"
                );
            }
            
            // Validate state transition
            if (emergency.getStatus() != EmergencyStatus.DISPATCHED) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot mark arrival. Current status: " + emergency.getStatus() + " (expected: DISPATCHED)"
                );
            }
            
            // Transition to AT_PATIENT
            emergency.setStatus(EmergencyStatus.AT_PATIENT);
            emergencyRepository.save(emergency);
            
            log.info("Driver {} marked arrival at patient for emergency {}", driverId, id);
            
            return ResponseEntity.ok(Map.of(
                "message", "Arrival at patient location recorded",
                "emergencyId", id,
                "status", "AT_PATIENT",
                "timestamp", LocalDateTime.now()
            ));
            
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error marking arrival for emergency {}: {}", id, e.getMessage(), e);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to mark arrival. Please try again."
            );
        }
    }

    /**
     * Driver reports patient loaded and starting transport to hospital.
     * 
     * POST /api/emergencies/{id}/pickup
     * 
     * Transitions: AT_PATIENT → TO_HOSPITAL
     * 
     * AUTHORIZATION: Must be the assigned driver
     */
    @PostMapping("/{id}/pickup")
    public ResponseEntity<Map<String, Object>> markPatientPickup(@PathVariable Long id) {
        authGuard.requireVerifiedDriver();
        
        Long driverId = AuthContext.get().getUserId();
        
        try {
            Emergency emergency = emergencyRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, 
                        "Emergency not found: " + id
                    ));
            
            // Verify driver is assigned to this emergency
            List<EmergencyAssignment> assignments = assignmentRepository.findByEmergencyId(id);
            boolean isAssignedDriver = assignments.stream()
                    .anyMatch(a -> driverId.equals(a.getDriverId()) 
                                && a.getStatus() == EmergencyAssignmentStatus.ACCEPTED);
            
            if (!isAssignedDriver) {
                throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not assigned to this emergency"
                );
            }
            
            // Validate state transition
            if (emergency.getStatus() != EmergencyStatus.AT_PATIENT) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot mark pickup. Current status: " + emergency.getStatus() + " (expected: AT_PATIENT)"
                );
            }
            
            // Transition to TO_HOSPITAL
            emergency.setStatus(EmergencyStatus.TO_HOSPITAL);
            emergencyRepository.save(emergency);
            
            log.info("Driver {} marked patient pickup for emergency {}", driverId, id);
            
            return ResponseEntity.ok(Map.of(
                "message", "Patient loaded, en route to hospital",
                "emergencyId", id,
                "status", "TO_HOSPITAL",
                "timestamp", LocalDateTime.now()
            ));
            
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error marking pickup for emergency {}: {}", id, e.getMessage(), e);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to mark pickup. Please try again."
            );
        }
    }


    @PostMapping("/{id}/complete")
    public ResponseEntity<?> completeEmergency(@PathVariable Long id) {

        if (!systemReadiness.isReady()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "System recovering, retry shortly"
            );
        }

        authGuard.requireVerifiedDriver();

        assignmentService.completeEmergency(id);

        return ResponseEntity.ok().build();
    }

    /**
     * Track ambulance location in real-time for patient app.
     * 
     * GET /api/emergencies/{id}/track
     * 
     * REAL-TIME TRACKING:
     * - Patient app should poll this endpoint every 3-5 seconds
     * - Returns ambulance GPS location, distance, and ETA
     * - Works with simple HTTP polling (no WebSockets needed for MVP)
     * 
     * AUTHORIZATION:
     * - Must be authenticated (patient who created the emergency)
     * - Future: Verify caller is the emergency creator
     */
    @GetMapping("/{id}/track")
    public ResponseEntity<Map<String, Object>> trackEmergency(@PathVariable Long id) {
        authGuard.requireAuthenticated();
        
        Long userId = AuthContext.get().getUserId();
        
        try {
            // Get emergency
            Emergency emergency = emergencyRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, 
                        "Emergency not found: " + id
                    ));
            
            // Authorization: Verify user created this emergency
            if (!emergency.getUserId().equals(userId)) {
                throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can only track your own emergencies"
                );
            }
            
            Map<String, Object> trackingData = new HashMap<>();
            trackingData.put("emergencyId", id);
            trackingData.put("status", emergency.getStatus().toString());
            trackingData.put("patientLat", emergency.getLatitude());
            trackingData.put("patientLng", emergency.getLongitude());
            trackingData.put("emergencyType", emergency.getType());
            trackingData.put("severity", emergency.getSeverity());
            
            // Check if emergency has been assigned
            List<EmergencyAssignment> assignments = assignmentRepository.findByEmergencyId(id);
            
            if (assignments.isEmpty()) {
                trackingData.put("message", "Searching for nearest ambulance...");
                trackingData.put("ambulanceAssigned", false);
                log.debug("Emergency {} not yet assigned", id);
                return ResponseEntity.ok(trackingData);
            }
            
            // Get active assignment (ASSIGNED or ACCEPTED)
            Optional<EmergencyAssignment> activeAssignment = assignments.stream()
                    .filter(a -> a.getStatus() == EmergencyAssignmentStatus.ASSIGNED 
                              || a.getStatus() == EmergencyAssignmentStatus.ACCEPTED)
                    .findFirst();
            
            if (activeAssignment.isEmpty()) {
                trackingData.put("message", "Assignment in progress...");
                trackingData.put("ambulanceAssigned", false);
                log.debug("Emergency {} has no active assignment", id);
                return ResponseEntity.ok(trackingData);
            }
            
            EmergencyAssignment assignment = activeAssignment.get();
            trackingData.put("ambulanceAssigned", true);
            trackingData.put("ambulanceCode", assignment.getAmbulance().getCode());
            trackingData.put("assignmentStatus", assignment.getStatus().toString());
            
            // Check if driver has accepted
            if (assignment.getStatus() == EmergencyAssignmentStatus.ASSIGNED) {
                trackingData.put("message", "Waiting for driver to accept...");
                log.debug("Emergency {} assigned but not yet accepted by driver", id);
                return ResponseEntity.ok(trackingData);
            }
            
            // Driver accepted - get live location
            if (assignment.getDriverId() == null) {
                trackingData.put("message", "Driver information unavailable");
                log.warn("Emergency {} has accepted assignment but no driver_id", id);
                return ResponseEntity.ok(trackingData);
            }
            
            // Get driver's current session for live GPS
            Optional<DriverSession> sessionOpt = driverSessionService.getCurrentSession(assignment.getDriverId());
            
            if (sessionOpt.isEmpty()) {
                trackingData.put("message", "Driver session not found");
                log.warn("Emergency {} driver {} has no active session", id, assignment.getDriverId());
                return ResponseEntity.ok(trackingData);
            }
            
            DriverSession session = sessionOpt.get();
            
            // Check if driver's GPS is stale
            if (session.getCurrentLat() == null || session.getCurrentLng() == null) {
                trackingData.put("message", "Waiting for driver GPS update...");
                log.debug("Emergency {} driver has no location data yet", id);
                return ResponseEntity.ok(trackingData);
            }
            
            if (session.isStale()) {
                trackingData.put("message", "Driver GPS signal lost. Reconnecting...");
                trackingData.put("warning", "Last GPS update was more than 30 seconds ago");
                log.warn("Emergency {} driver {} has stale GPS (last: {})", 
                    id, assignment.getDriverId(), session.getLastHeartbeat());
            }
            
            // Calculate distance and ETA
            double distanceKm = GeoUtil.distanceKm(
                session.getCurrentLat(), 
                session.getCurrentLng(),
                emergency.getLatitude(), 
                emergency.getLongitude()
            );
            
            // ETA calculation: Assume 30 km/h average speed in city
            int etaMinutes = (int) Math.ceil(distanceKm / 0.5); // 30 km/h = 0.5 km/min
            
            // Add live tracking data
            trackingData.put("driverLat", session.getCurrentLat());
            trackingData.put("driverLng", session.getCurrentLng());
            trackingData.put("distanceKm", Math.round(distanceKm * 100.0) / 100.0); // Round to 2 decimals
            trackingData.put("etaMinutes", etaMinutes);
            trackingData.put("lastGpsUpdate", session.getLocationUpdatedAt());
            trackingData.put("driverStatus", session.getStatus().toString());
            trackingData.put("message", "Ambulance en route");
            
            log.debug("Tracking emergency {}: Distance {}km, ETA {}min, GPS age: {} seconds",
                id, distanceKm, etaMinutes,
                session.getLastHeartbeat() != null 
                    ? java.time.Duration.between(session.getLastHeartbeat(), LocalDateTime.now()).getSeconds()
                    : "N/A");
            
            return ResponseEntity.ok(trackingData);
            
        } catch (ResponseStatusException e) {
            throw e; // Re-throw HTTP errors
        } catch (Exception e) {
            log.error("Error tracking emergency {}: {}", id, e.getMessage(), e);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to retrieve tracking data. Please try again."
            );
        }
    }


}
