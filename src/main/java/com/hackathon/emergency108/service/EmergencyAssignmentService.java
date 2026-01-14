package com.hackathon.emergency108.service;

import com.hackathon.emergency108.entity.*;
import com.hackathon.emergency108.event.AssignmentEvent;
import com.hackathon.emergency108.event.DomainEventPublisher;
import com.hackathon.emergency108.metrics.DomainMetrics;
import com.hackathon.emergency108.repository.AmbulanceRepository;
import com.hackathon.emergency108.repository.EmergencyAssignmentRepository;
import com.hackathon.emergency108.repository.EmergencyRepository;
import com.hackathon.emergency108.util.EmergencyAssignmentEmergencyConsistency;
import com.hackathon.emergency108.util.EmergencyAssignmentStateMachine;
import com.hackathon.emergency108.util.EmergencyStateMachine;
import com.hackathon.emergency108.util.InvalidAssignmentStateException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmergencyAssignmentService {

    private final EmergencyAssignmentRepository assignmentRepository;
    private final AmbulanceRepository ambulanceRepository;
    private final EmergencyDispatchService emergencyDispatchService;
    private final EmergencyRepository emergencyRepository;
    private final DomainEventPublisher eventPublisher;
    private final DomainMetrics metrics;

    private static final Logger log =
            LoggerFactory.getLogger(EmergencyAssignmentService.class);




    public EmergencyAssignmentService(EmergencyAssignmentRepository assignmentRepository,
                                      AmbulanceRepository ambulanceRepository,
                                      EmergencyDispatchService emergencyDispatchService,
                                      EmergencyRepository emergencyRepository,
                                      DomainEventPublisher eventPublisher,
                                      DomainMetrics metrics) {
        this.metrics = metrics;
        this.eventPublisher = eventPublisher;
        this.emergencyRepository = emergencyRepository;
        this.emergencyDispatchService = emergencyDispatchService;
        this.ambulanceRepository = ambulanceRepository;
        this.assignmentRepository = assignmentRepository;
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
                LocalDateTime.now().plusMinutes(2)
        );

        // 3️⃣ Validate AFTER state changes
        validateAssignmentEmergencyConsistency(assignment, emergency);

        eventPublisher.publish(
                new AssignmentEvent(
                        emergency.getId(),
                        ambulance.getId(),
                        "ASSIGNMENT_ASSIGNED",
                        "Ambulance " + ambulance.getCode() + " assigned"
                )
        );


        // 4️⃣ Persist assignment
        return assignmentRepository.save(assignment);
    }


    private void validateEmergencyTransition(
            Emergency emergency,
            EmergencyStatus next
    ) {
        EmergencyStatus current = emergency.getStatus();

        if (!EmergencyStateMachine.canTransition(current, next)) {
            throw new InvalidAssignmentStateException(
                    "Invalid emergency transition: " + current + " → " + next
            );
        }
    }

    private void validateAssignmentTransition(
            EmergencyAssignment assignment,
            EmergencyAssignmentStatus next
    ) {
        EmergencyAssignmentStatus current = assignment.getStatus();

        if (!EmergencyAssignmentStateMachine.canTransition(current, next)) {
            throw new InvalidAssignmentStateException(
                    "Invalid assignment transition: " + current + " → " + next
            );
        }
    }

    private void validateAssignmentEmergencyConsistency(
            EmergencyAssignment assignment,
            Emergency emergency
    ) {
        EmergencyAssignmentEmergencyConsistency.validate(
                assignment.getStatus(),
                emergency.getStatus()
        );
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
                    rejectedAmbulance.getId()
            );
        }


        // 3️⃣ Find next ambulance
        List<Long> triedAmbulanceIds =
                assignmentRepository.findByEmergencyId(emergency.getId())
                        .stream()
                        .map(a -> a.getAmbulance().getId())
                        .toList();

        Ambulance next = emergencyDispatchService
                .assignNearestAmbulanceExcluding(
                        emergency.getLatitude(),
                        emergency.getLongitude(),
                        triedAmbulanceIds
                );


        // 4️⃣ Create new assignment
        EmergencyAssignment newAssignment = assign(emergency, next);
        validateAssignmentEmergencyConsistency(newAssignment, emergency);


        return next;
    }

    @Transactional
    public void handleTimeouts() {

        List<EmergencyAssignment> expired =
                assignmentRepository.findByStatusAndResponseDeadlineBefore(
                        EmergencyAssignmentStatus.ASSIGNED,
                        LocalDateTime.now()
                );

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
                        ambulance.getId()
                );
            }


            eventPublisher.publish(
                    new AssignmentEvent(
                            emergency.getId(),
                            ambulance.getId(),
                            "ASSIGNMENT_TIMED_OUT",
                            "Assignment timed out"
                    )
            );


            try {
                List<Long> tried =
                        assignmentRepository.findByEmergencyId(emergency.getId())
                                .stream()
                                .map(a -> a.getAmbulance().getId())
                                .toList();

                Ambulance next =
                        emergencyDispatchService.assignNearestAmbulanceExcluding(
                                emergency.getLatitude(),
                                emergency.getLongitude(),
                                tried
                        );

                assign(emergency, next);

            } catch (RuntimeException ex) {
                emergency.setStatus(EmergencyStatus.UNASSIGNED);
                emergency.setStatusUpdatedAt(LocalDateTime.now());
                emergencyRepository.save(emergency);
            }
            metrics.assignmentTimeout();

        }
    }


    @Transactional(noRollbackFor = InvalidAssignmentStateException.class)
    public void respondToAssignment(
            Long emergencyId,
            boolean accepted
    ) {

        Emergency emergencyFromRepository = emergencyRepository
                .findById(emergencyId)
                .orElseThrow();


        if (emergencyFromRepository.getStatus() == EmergencyStatus.COMPLETED) {
            throw new InvalidAssignmentStateException(
                    "Emergency already completed"
            );
        }


        EmergencyAssignment assignment =
                assignmentRepository.findActiveAssignmentForUpdate(emergencyId)
                        .orElseThrow(() ->
                                new InvalidAssignmentStateException(
                                        "No active ASSIGNED assignment for this emergency"
                                )
                        );
        if (assignment.getStatus() != EmergencyAssignmentStatus.ASSIGNED) {
            throw new InvalidAssignmentStateException(
                    "Assignment is no longer active"
            );
        }


        Ambulance ambulance = assignment.getAmbulance();
        Emergency emergency = assignment.getEmergency();

        if (accepted) {

            // ✅ Driver accepted
            validateAssignmentTransition(assignment, EmergencyAssignmentStatus.ACCEPTED);
            assignment.setStatus(EmergencyAssignmentStatus.ACCEPTED);
            assignment.setAcceptedAt(LocalDateTime.now());
            assignmentRepository.save(assignment);

            validateEmergencyTransition(emergency, EmergencyStatus.DISPATCHED);
            emergency.setStatus(EmergencyStatus.DISPATCHED);
            emergencyRepository.save(emergency);

            validateAssignmentEmergencyConsistency(assignment, emergency);

            eventPublisher.publish(
                    new AssignmentEvent(
                            emergency.getId(),
                            ambulance.getId(),
                            "ASSIGNMENT_ACCEPTED",
                            "Driver accepted assignment"
                    )
            );
            metrics.assignmentAccepted();



        } else {

            // ❌ Driver rejected
            validateAssignmentTransition(assignment, EmergencyAssignmentStatus.REJECTED);
            assignment.setStatus(EmergencyAssignmentStatus.REJECTED);
            assignment.setRejectedAt(LocalDateTime.now());
            assignmentRepository.save(assignment);

            validateAssignmentEmergencyConsistency(assignment, emergency);

            try {
                ambulance.setStatus(AmbulanceStatus.AVAILABLE);
                ambulanceRepository.save(ambulance);
            } catch (ObjectOptimisticLockingFailureException ex) {
                log.trace(
                        "Ambulance {} already updated by another transaction",
                        ambulance.getId()
                );
            }

            metrics.assignmentRejected();


            // Try next ambulance
            List<Long> triedAmbulanceIds =
                    assignmentRepository.findByEmergencyId(emergency.getId())
                            .stream()
                            .map(a -> a.getAmbulance().getId())
                            .toList();

            try {
                Ambulance next =
                        emergencyDispatchService.assignNearestAmbulanceExcluding(
                                emergency.getLatitude(),
                                emergency.getLongitude(),
                                triedAmbulanceIds
                        );

                assign(emergency, next);

            } catch (RuntimeException ex) {

                validateEmergencyTransition(emergency, EmergencyStatus.UNASSIGNED);
                emergency.setStatus(EmergencyStatus.UNASSIGNED);
                emergency.setStatusUpdatedAt(LocalDateTime.now());
                emergencyRepository.save(emergency);

                validateAssignmentEmergencyConsistency(assignment, emergency);

                throw new InvalidAssignmentStateException(
                        "All ambulances rejected or unavailable for this emergency"
                );
            }



        }
    }

    @Transactional
    public void completeEmergency(Long emergencyId) {

        EmergencyAssignment assignment =
                assignmentRepository
                        .findByEmergencyIdAndStatus(
                                emergencyId,
                                EmergencyAssignmentStatus.ACCEPTED
                        )
                        .orElseThrow(() ->
                                new InvalidAssignmentStateException(
                                        "No ACCEPTED assignment to complete"
                                )
                        );

        Emergency emergency = assignment.getEmergency();
        Ambulance ambulance = assignment.getAmbulance();

        // 1️⃣ Complete assignment
        validateAssignmentTransition(assignment, EmergencyAssignmentStatus.COMPLETED);
        assignment.setStatus(EmergencyAssignmentStatus.COMPLETED);
        assignment.setCompletedAt(LocalDateTime.now());
        assignmentRepository.save(assignment);

        // 2️⃣ Complete emergency
        validateEmergencyTransition(emergency, EmergencyStatus.COMPLETED);
        emergency.setStatus(EmergencyStatus.COMPLETED);
        emergencyRepository.save(emergency);

        validateAssignmentEmergencyConsistency(assignment, emergency);

        eventPublisher.publish(
                new AssignmentEvent(
                        emergency.getId(),
                        ambulance.getId(),
                        "ASSIGNMENT_COMPLETED",
                        "Emergency completed, ambulance released"
                )
        );



        // 3️⃣ Free ambulance
        try {
            ambulance.setStatus(AmbulanceStatus.AVAILABLE);
            ambulanceRepository.save(ambulance);
        } catch (ObjectOptimisticLockingFailureException ex) {
            log.trace(
                    "Ambulance {} already updated by another transaction",
                    ambulance.getId()
            );
        }
        metrics.assignmentCompleted();

    }

}
