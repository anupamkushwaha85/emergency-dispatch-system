package com.emergency.emergency108.service;

import com.emergency.emergency108.auth.security.AuthContext;
import com.emergency.emergency108.entity.*;
import com.emergency.emergency108.event.AssignmentEvent;
import com.emergency.emergency108.event.DomainEventPublisher;
import com.emergency.emergency108.exception.NoAmbulancesAvailableException;
import com.emergency.emergency108.metrics.DomainMetrics;
import com.emergency.emergency108.repository.AmbulanceRepository;
import com.emergency.emergency108.repository.EmergencyAssignmentRepository;
import com.emergency.emergency108.repository.EmergencyRepository;
import com.emergency.emergency108.util.EmergencyAssignmentEmergencyConsistency;
import com.emergency.emergency108.util.EmergencyAssignmentStateMachine;
import com.emergency.emergency108.util.EmergencyStateMachine;
import com.emergency.emergency108.util.InvalidAssignmentStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class EmergencyAssignmentService {

    private final EmergencyAssignmentRepository assignmentRepository;
    private final AmbulanceRepository ambulanceRepository;
    private final EmergencyDispatchService emergencyDispatchService;
    private final EmergencyRepository emergencyRepository;
    private final DomainEventPublisher eventPublisher;
    private final DomainMetrics metrics;
    private final DriverSessionService driverSessionService;

    private static final Logger log = LoggerFactory.getLogger(EmergencyAssignmentService.class);

    public EmergencyAssignmentService(EmergencyAssignmentRepository assignmentRepository,
            AmbulanceRepository ambulanceRepository,
            EmergencyDispatchService emergencyDispatchService,
            EmergencyRepository emergencyRepository,
            DomainEventPublisher eventPublisher,
            DomainMetrics metrics,
            DriverSessionService driverSessionService) {
        this.metrics = metrics;
        this.eventPublisher = eventPublisher;
        this.emergencyRepository = emergencyRepository;
        this.emergencyDispatchService = emergencyDispatchService;
        this.ambulanceRepository = ambulanceRepository;
        this.assignmentRepository = assignmentRepository;
        this.driverSessionService = driverSessionService;
    }

    public boolean isAlreadyAssigned(Long emergencyId) {
        return assignmentRepository.existsByEmergencyId(emergencyId);
    }

    @Transactional
    public EmergencyAssignment assign(Emergency emergency, Ambulance ambulance) {

        // 1️⃣ Mark ambulance BUSY immediately
        ambulance.setStatus(AmbulanceStatus.BUSY);
        ambulance.setUpdatedAt(LocalDateTime.now());
        ambulanceRepository.save(ambulance);

        // 2️⃣ Create assignment
        EmergencyAssignment assignment = new EmergencyAssignment();
        assignment.setEmergency(emergency);
        assignment.setAmbulance(ambulance);
        assignment.setStatus(EmergencyAssignmentStatus.ASSIGNED);
        assignment.setAssignedAt(LocalDateTime.now());
        assignment.setResponseDeadline(
                LocalDateTime.now().plusMinutes(2));

        // 3️⃣ Validate AFTER state changes
        validateAssignmentEmergencyConsistency(assignment, emergency);

        eventPublisher.publish(
                new AssignmentEvent(
                        emergency.getId(),
                        ambulance.getId(),
                        "ASSIGNMENT_ASSIGNED",
                        "Ambulance " + ambulance.getCode() + " assigned"));

        // 4️⃣ Persist assignment
        return assignmentRepository.save(assignment);
    }

    private void validateEmergencyTransition(
            Emergency emergency,
            EmergencyStatus next) {
        EmergencyStatus current = emergency.getStatus();

        if (!EmergencyStateMachine.canTransition(current, next)) {
            throw new InvalidAssignmentStateException(
                    "Invalid emergency transition: " + current + " → " + next);
        }
    }

    private void validateAssignmentTransition(
            EmergencyAssignment assignment,
            EmergencyAssignmentStatus next) {
        EmergencyAssignmentStatus current = assignment.getStatus();

        if (!EmergencyAssignmentStateMachine.canTransition(current, next)) {
            throw new InvalidAssignmentStateException(
                    "Invalid assignment transition: " + current + " → " + next);
        }
    }

    private void validateAssignmentEmergencyConsistency(
            EmergencyAssignment assignment,
            Emergency emergency) {
        EmergencyAssignmentEmergencyConsistency.validate(
                assignment.getStatus(),
                emergency.getStatus());
    }

    @Transactional
    public void markEmergencyInProgress(Emergency emergency) {

        validateEmergencyTransition(emergency, EmergencyStatus.IN_PROGRESS);

        emergency.setStatus(EmergencyStatus.IN_PROGRESS);
        emergency.setStatusUpdatedAt(LocalDateTime.now());

        emergencyRepository.save(emergency);
    }

    @Transactional
    public Ambulance rejectAndRetry(Emergency emergency, Ambulance rejectedAmbulance) {

        // 1️⃣ Mark previous assignment as REJECTED
        EmergencyAssignment last = assignmentRepository
                .findTopByEmergencyIdOrderByAssignedAtDesc(emergency.getId())
                .orElseThrow();

        last.setStatus(EmergencyAssignmentStatus.REJECTED);
        assignmentRepository.save(last);

        // 2️⃣ Free ambulance
        try {
            rejectedAmbulance.setStatus(AmbulanceStatus.AVAILABLE);
            ambulanceRepository.save(rejectedAmbulance);
        } catch (ObjectOptimisticLockingFailureException ex) {
            log.trace(
                    "Ambulance {} already updated by another transaction",
                    rejectedAmbulance.getId());
        }

        // 3️⃣ Find next ambulance
        // Dispatch to next available driver
        log.info("Re-dispatching emergency {} after driver rejection", emergency.getId());
        emergencyDispatchService.dispatchToNearestAvailableAmbulance(emergency.getId());

        return null;
    }

    @Transactional
    public void handleTimeouts() {

        List<EmergencyAssignment> expired = assignmentRepository.findByStatusAndResponseDeadlineBefore(
                EmergencyAssignmentStatus.ASSIGNED,
                LocalDateTime.now());

        for (EmergencyAssignment assignment : expired) {

            // 🔒 lock assignment
            assignmentRepository.findById(assignment.getId()).orElseThrow();

            if (assignment.getStatus() != EmergencyAssignmentStatus.ASSIGNED) {
                continue;
            }

            Emergency emergency = assignment.getEmergency();
            Ambulance ambulance = assignment.getAmbulance();

            assignment.setStatus(EmergencyAssignmentStatus.REJECTED);
            assignment.setRejectedAt(LocalDateTime.now());
            assignmentRepository.save(assignment);

            try {
                ambulance.setStatus(AmbulanceStatus.AVAILABLE);
                ambulanceRepository.save(ambulance);
            } catch (ObjectOptimisticLockingFailureException ex) {
                log.trace(
                        "Ambulance {} already updated by another transaction",
                        ambulance.getId());
            }

            eventPublisher.publish(
                    new AssignmentEvent(
                            emergency.getId(),
                            ambulance.getId(),
                            "ASSIGNMENT_TIMED_OUT",
                            "Assignment timed out"));

            try {
                // BUG FIX: Reset to CREATED so dispatch service accepts it
                emergency.setStatus(EmergencyStatus.CREATED);
                emergencyRepository.save(emergency);

                // Re-dispatch to next available driver
                emergencyDispatchService.dispatchToNearestAvailableAmbulance(emergency.getId());

            } catch (RuntimeException ex) {
                // If dispatch fails (no drivers), keep it as CREATED so it can be picked up
                // later
                // or marked UNASSIGNED if that's the intended "pool" state.
                // Using CREATED ensures it's visible as "New"
                emergency.setStatus(EmergencyStatus.CREATED);
                emergency.setStatusUpdatedAt(LocalDateTime.now());
                emergencyRepository.save(emergency);
            }
            metrics.assignmentTimeout();

        }
    }

    @Transactional(noRollbackFor = InvalidAssignmentStateException.class)
    public void respondToAssignment(
            Long emergencyId,
            boolean accepted) {

        // 🔐 Get authenticated driver
        Long driverId = AuthContext.get().getUserId();

        Emergency emergencyFromRepository = emergencyRepository
                .findById(emergencyId)
                .orElseThrow();

        if (emergencyFromRepository.getStatus() == EmergencyStatus.COMPLETED) {
            throw new InvalidAssignmentStateException(
                    "Emergency already completed");
        }

        EmergencyAssignment assignment = assignmentRepository.findActiveAssignmentForUpdate(emergencyId)
                .orElseThrow(() -> new InvalidAssignmentStateException(
                        "No active ASSIGNED assignment for this emergency"));
        if (assignment.getStatus() != EmergencyAssignmentStatus.ASSIGNED) {
            throw new InvalidAssignmentStateException(
                    "Assignment is no longer active");
        }

        // 🔐 VALIDATE: Driver must be operating this ambulance
        Ambulance ambulance = assignment.getAmbulance();
        if (!driverSessionService.isDriverOperatingAmbulance(driverId, ambulance.getId())) {
            throw new InvalidAssignmentStateException(
                    String.format(
                            "You are not authorized to respond to this assignment. " +
                                    "This emergency is assigned to ambulance %s which you are not currently operating.",
                            ambulance.getCode()));
        }

        Emergency emergency = assignment.getEmergency();

        if (accepted) {

            // 🔐 INVARIANT CHECK: Driver must be ONLINE to accept
            driverSessionService.validateCanAcceptEmergency(driverId, ambulance.getId());

            // ✅ Driver accepted
            validateAssignmentTransition(assignment, EmergencyAssignmentStatus.ACCEPTED);
            assignment.setStatus(EmergencyAssignmentStatus.ACCEPTED);
            assignment.setAcceptedAt(LocalDateTime.now());
            assignment.setDriverId(driverId); // 🎯 Track which driver accepted
            assignmentRepository.save(assignment);

            validateEmergencyTransition(emergency, EmergencyStatus.DISPATCHED);
            emergency.setStatus(EmergencyStatus.DISPATCHED);
            emergencyRepository.save(emergency);

            validateAssignmentEmergencyConsistency(assignment, emergency);

            // Mark driver as ON_TRIP (centralized state transition)
            driverSessionService.markDriverOnTrip(driverId);

            eventPublisher.publish(
                    new AssignmentEvent(
                            emergency.getId(),
                            ambulance.getId(),
                            "ASSIGNMENT_ACCEPTED",
                            "Driver accepted assignment"));
            metrics.assignmentAccepted();

        } else {

            // 🔐 VALIDATE: Driver can reject from ONLINE status
            driverSessionService.validateRejection(driverId, ambulance.getId());

            // ❌ Driver rejected (stays ONLINE)
            validateAssignmentTransition(assignment, EmergencyAssignmentStatus.REJECTED);
            assignment.setStatus(EmergencyAssignmentStatus.REJECTED);
            assignment.setRejectedAt(LocalDateTime.now());
            assignment.setDriverId(driverId); // 🎯 Track which driver rejected
            assignmentRepository.save(assignment);

            validateAssignmentEmergencyConsistency(assignment, emergency);

            try {
                ambulance.setStatus(AmbulanceStatus.AVAILABLE);
                ambulanceRepository.save(ambulance);
            } catch (ObjectOptimisticLockingFailureException ex) {
                log.trace(
                        "Ambulance {} already updated by another transaction",
                        ambulance.getId());
            }

            metrics.assignmentRejected();

            // Try next ambulance
            List<Long> triedAmbulanceIds = assignmentRepository.findByEmergencyId(emergency.getId())
                    .stream()
                    .map(a -> a.getAmbulance().getId())
                    .toList();

            try {
                // Re-dispatch to next available driver
                emergencyDispatchService.dispatchToNearestAvailableAmbulance(emergency.getId());

            } catch (RuntimeException ex) {

                validateEmergencyTransition(emergency, EmergencyStatus.UNASSIGNED);
                emergency.setStatus(EmergencyStatus.UNASSIGNED);
                emergency.setStatusUpdatedAt(LocalDateTime.now());
                emergencyRepository.save(emergency);

                validateAssignmentEmergencyConsistency(assignment, emergency);

                throw new InvalidAssignmentStateException(
                        "All ambulances rejected or unavailable for this emergency");
            }

        }
    }

    @Transactional
    public void completeEmergency(Long emergencyId) {

        // 🔐 Get authenticated driver
        Long driverId = AuthContext.get().getUserId();

        EmergencyAssignment assignment = assignmentRepository
                .findByEmergencyIdAndStatus(
                        emergencyId,
                        EmergencyAssignmentStatus.ACCEPTED)
                .orElseThrow(() -> new InvalidAssignmentStateException(
                        "No ACCEPTED assignment to complete"));

        Emergency emergency = assignment.getEmergency();
        Ambulance ambulance = assignment.getAmbulance();

        // 🔐 VALIDATE: Driver must be the one who accepted this assignment
        if (assignment.getDriverId() != null && !assignment.getDriverId().equals(driverId)) {
            throw new InvalidAssignmentStateException(
                    "You are not authorized to complete this emergency. " +
                            "It was accepted by a different driver.");
        }

        // 1️⃣ Complete assignment
        validateAssignmentTransition(assignment, EmergencyAssignmentStatus.COMPLETED);
        assignment.setStatus(EmergencyAssignmentStatus.COMPLETED);
        assignment.setCompletedAt(LocalDateTime.now());
        assignmentRepository.save(assignment);

        // 2️⃣ Complete emergency
        // Production: Only allow completion from TO_HOSPITAL status (patient delivered)
        if (emergency.getStatus() != EmergencyStatus.TO_HOSPITAL
                && emergency.getStatus() != EmergencyStatus.DISPATCHED) {
            // Allow DISPATCHED for backward compatibility during migration
            throw new InvalidAssignmentStateException(
                    "Emergency must be in TO_HOSPITAL status to complete. Current: " + emergency.getStatus());
        }

        validateEmergencyTransition(emergency, EmergencyStatus.COMPLETED);
        emergency.setStatus(EmergencyStatus.COMPLETED);
        emergencyRepository.save(emergency);

        validateAssignmentEmergencyConsistency(assignment, emergency);

        // 3️⃣ Mark driver back as ONLINE (available for next emergency)
        try {
            driverSessionService.markDriverOnline(driverId);
        } catch (Exception e) {
            log.warn("Failed to mark driver {} as ONLINE after completion: {}",
                    driverId, e.getMessage());
            // Don't fail the entire transaction if driver session update fails
        }

        eventPublisher.publish(
                new AssignmentEvent(
                        emergency.getId(),
                        ambulance.getId(),
                        "ASSIGNMENT_COMPLETED",
                        "Emergency completed, ambulance released"));

        // 4️⃣ Free ambulance
        try {
            ambulance.setStatus(AmbulanceStatus.AVAILABLE);
            ambulanceRepository.save(ambulance);
        } catch (ObjectOptimisticLockingFailureException ex) {
            log.trace(
                    "Ambulance {} already updated by another transaction",
                    ambulance.getId());
        }
        metrics.assignmentCompleted();

    }

    /**
     * Driver accepts an emergency assignment.
     * Updates assignment status to ACCEPTED and driver session to ON_TRIP.
     * 
     * @param emergencyId Emergency ID
     * @param driverId    Driver ID
     * @return Updated assignment
     */
    @Transactional
    public EmergencyAssignment acceptEmergency(Long emergencyId, Long driverId) {
        // Find active assignment for this emergency and driver
        EmergencyAssignment assignment = assignmentRepository
                .findByEmergencyIdAndDriverIdAndStatus(emergencyId, driverId, EmergencyAssignmentStatus.ASSIGNED)
                .orElseThrow(
                        () -> new IllegalStateException("No active assignment found for this emergency and driver"));

        Emergency emergency = assignment.getEmergency();

        // Validate emergency status
        if (emergency.getStatus() != EmergencyStatus.DISPATCHED) {
            throw new IllegalStateException("Emergency is not in DISPATCHED status");
        }

        LocalDateTime now = LocalDateTime.now();

        // Calculate response time
        if (assignment.getAssignedAt() != null) {
            long secondsElapsed = java.time.temporal.ChronoUnit.SECONDS.between(assignment.getAssignedAt(), now);
            assignment.setResponseTimeSeconds((int) secondsElapsed);
        }

        // Update assignment status
        assignment.setStatus(EmergencyAssignmentStatus.ACCEPTED);
        assignment.setAcceptedAt(now);
        assignmentRepository.save(assignment);

        // Update emergency status
        emergency.setStatus(EmergencyStatus.IN_PROGRESS);
        emergencyRepository.save(emergency);

        // Update driver session to ON_TRIP
        DriverSession session = driverSessionService.getActiveSession(driverId);
        if (session != null) {
            session.setStatus(DriverSessionStatus.ON_TRIP);
            driverSessionService.saveSession(session);
        }

        // Update ambulance status to BUSY
        Ambulance ambulance = assignment.getAmbulance();
        if (ambulance != null) {
            ambulance.setStatus(AmbulanceStatus.BUSY);
            ambulanceRepository.save(ambulance);
        }

        log.info("Driver {} accepted emergency {} - status: DISPATCHED -> IN_PROGRESS", driverId, emergencyId);

        eventPublisher.publish(
                new AssignmentEvent(
                        emergencyId,
                        ambulance != null ? ambulance.getId() : null,
                        "ASSIGNMENT_ACCEPTED",
                        "Driver accepted emergency"));

        metrics.assignmentCompleted();

        return assignment;
    }

    /**
     * Driver rejects an emergency assignment.
     * Updates assignment status to REJECTED and re-dispatches to next driver.
     * 
     * @param emergencyId Emergency ID
     * @param driverId    Driver ID
     */
    @Transactional
    public void rejectEmergency(Long emergencyId, Long driverId) {
        // Find active assignment for this emergency and driver
        EmergencyAssignment assignment = assignmentRepository
                .findByEmergencyIdAndDriverIdAndStatus(emergencyId, driverId, EmergencyAssignmentStatus.ASSIGNED)
                .orElseThrow(
                        () -> new IllegalStateException("No active assignment found for this emergency and driver"));

        LocalDateTime now = LocalDateTime.now();

        // Calculate response time
        if (assignment.getAssignedAt() != null) {
            long secondsElapsed = java.time.temporal.ChronoUnit.SECONDS.between(assignment.getAssignedAt(), now);
            assignment.setResponseTimeSeconds((int) secondsElapsed);
        }

        // Update assignment status
        assignment.setStatus(EmergencyAssignmentStatus.REJECTED);
        assignment.setRejectedAt(now);
        assignment.setCancellationReason("Driver manually rejected");
        assignmentRepository.save(assignment);

        // Release driver back to ONLINE status
        driverSessionService.markDriverOnline(driverId);
        log.info("Driver {} session released back to ONLINE", driverId);

        log.info("Driver {} rejected emergency {}", driverId, emergencyId);

        eventPublisher.publish(
                new AssignmentEvent(
                        emergencyId,
                        assignment.getAmbulance() != null ? assignment.getAmbulance().getId() : null,
                        "ASSIGNMENT_REJECTED",
                        "Driver rejected emergency"));

        // Reset emergency status to CREATED for re-dispatch
        Emergency emergency = emergencyRepository.findById(emergencyId)
                .orElseThrow(() -> new IllegalStateException("Emergency not found: " + emergencyId));
        emergency.setStatus(EmergencyStatus.CREATED);
        emergencyRepository.saveAndFlush(emergency);
        log.info("Reset emergency {} status to CREATED for re-dispatch", emergencyId);

        // Re-dispatch to next available driver
        try {
            emergencyDispatchService.dispatchToNearestAvailableAmbulance(emergencyId);
            log.info("Re-dispatching emergency {} to next available driver", emergencyId);
        } catch (Exception e) {
            log.error("Failed to re-dispatch emergency {} after rejection: {}", emergencyId, e.getMessage());
        }
    }

    /**
     * Get current active assignment for driver.
     * 
     * @param driverId Driver ID
     * @return Current assignment or null if none
     */
    @Transactional(readOnly = true)
    public EmergencyAssignment getCurrentAssignment(Long driverId) {
        // Try to find ASSIGNED status first
        Optional<EmergencyAssignment> assigned = assignmentRepository.findByDriverIdAndStatus(
                driverId, EmergencyAssignmentStatus.ASSIGNED);

        if (assigned.isPresent()) {
            return assigned.get();
        }

        // If not ASSIGNED, try ACCEPTED
        return assignmentRepository.findByDriverIdAndStatus(
                driverId, EmergencyAssignmentStatus.ACCEPTED)
                .orElse(null);
    }

}
