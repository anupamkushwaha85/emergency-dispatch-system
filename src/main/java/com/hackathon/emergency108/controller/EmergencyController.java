package com.hackathon.emergency108.controller;


import com.hackathon.emergency108.auth.guard.AuthGuard;
import com.hackathon.emergency108.dto.EmergencyTimelineEvent;
import com.hackathon.emergency108.entity.Ambulance;
import com.hackathon.emergency108.entity.Emergency;
import com.hackathon.emergency108.entity.EmergencyStatus;
import com.hackathon.emergency108.metrics.DomainMetrics;
import com.hackathon.emergency108.repository.AmbulanceRepository;
import com.hackathon.emergency108.service.EmergencyAssignmentService;

import com.hackathon.emergency108.repository.EmergencyRepository;

import com.hackathon.emergency108.service.EmergencyDispatchService;
import com.hackathon.emergency108.service.EmergencyTimelineService;
import com.hackathon.emergency108.system.SystemReadiness;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import io.micrometer.core.instrument.Timer;

@RestController
@RequestMapping("/api/emergencies")
public class EmergencyController {

    private final EmergencyRepository emergencyRepository;
    private final EmergencyDispatchService emergencyDispatchService;

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


}
