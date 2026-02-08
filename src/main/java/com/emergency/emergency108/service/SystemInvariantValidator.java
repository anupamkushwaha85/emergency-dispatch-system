package com.emergency.emergency108.service;

import com.emergency.emergency108.entity.*;
import com.emergency.emergency108.repository.AmbulanceRepository;
import com.emergency.emergency108.repository.EmergencyAssignmentRepository;
import com.emergency.emergency108.repository.EmergencyRepository;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.time.LocalDateTime;
import java.util.List;

@Service
public class SystemInvariantValidator {

    private static final Logger log =
            LoggerFactory.getLogger(SystemInvariantValidator.class);

    private final EmergencyAssignmentRepository assignmentRepository;
    private final AmbulanceRepository ambulanceRepository;
    private final EmergencyRepository emergencyRepository;

    public SystemInvariantValidator(
            EmergencyAssignmentRepository assignmentRepository,
            AmbulanceRepository ambulanceRepository,
            EmergencyRepository emergencyRepository
    ) {
        this.assignmentRepository = assignmentRepository;
        this.ambulanceRepository = ambulanceRepository;
        this.emergencyRepository = emergencyRepository;
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void validateInvariants() {

        try {
            // 🚑 BUSY ambulance must have ASSIGNED or ACCEPTED assignment
            List<Ambulance> busyAmbulances =
                    ambulanceRepository.findByStatus(AmbulanceStatus.BUSY);

            for (Ambulance ambulance : busyAmbulances) {

                boolean hasActiveAssignment =
                        assignmentRepository.findByEmergencyId(ambulance.getId())
                                .stream()
                                .anyMatch(a ->
                                        a.getStatus() == EmergencyAssignmentStatus.ASSIGNED ||
                                                a.getStatus() == EmergencyAssignmentStatus.ACCEPTED
                                );

                if (!hasActiveAssignment) {
                    ambulance.setStatus(AmbulanceStatus.AVAILABLE);
                    ambulanceRepository.save(ambulance);
                }
            }

            // 🚨 IN_PROGRESS emergency must have ACCEPTED assignment
            List<Emergency> inProgress =
                    emergencyRepository.findByStatus(EmergencyStatus.IN_PROGRESS);

            for (Emergency emergency : inProgress) {

                boolean hasAcceptedAssignment =
                        assignmentRepository
                                .findByEmergencyIdAndStatus(
                                        emergency.getId(),
                                        EmergencyAssignmentStatus.ACCEPTED
                                )
                                .isPresent();

                if (!hasAcceptedAssignment) {
                    emergency.setStatus(EmergencyStatus.UNASSIGNED);
                    emergency.setStatusUpdatedAt(LocalDateTime.now());
                    emergencyRepository.save(emergency);
                }
            }
        }catch (ObjectOptimisticLockingFailureException ex){
            // ✔ Expected under concurrent self-healing
            // ✔ Another transaction fixed it first
            // ✔ Safe to ignore

            log.debug(
                    "Invariant validation skipped due to concurrent update: {}",
                    ex.getMessage()
            );
        }
    }
}
