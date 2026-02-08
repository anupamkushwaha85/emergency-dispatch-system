package com.emergency.emergency108.service;

import com.emergency.emergency108.entity.*;
import com.emergency.emergency108.event.DomainEventPublisher;
import com.emergency.emergency108.event.EmergencyEvent;
import com.emergency.emergency108.metrics.DomainMetrics;
import com.emergency.emergency108.repository.AmbulanceRepository;
import com.emergency.emergency108.repository.EmergencyAssignmentRepository;
import com.emergency.emergency108.repository.EmergencyRepository;
import com.emergency.emergency108.system.SystemReadiness;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StartupRecoveryService {

    private final EmergencyAssignmentRepository assignmentRepository;
    private final AmbulanceRepository ambulanceRepository;
    private final EmergencyRepository emergencyRepository;
    private final SystemReadiness systemReadiness;
    private final DomainEventPublisher eventPublisher;
    private final DomainMetrics metrics;

    public StartupRecoveryService(
            EmergencyAssignmentRepository assignmentRepository,
            AmbulanceRepository ambulanceRepository,
            EmergencyRepository emergencyRepository,
            SystemReadiness systemReadiness,
            DomainEventPublisher eventPublisher,
            DomainMetrics metrics
    ) {
        this.assignmentRepository = assignmentRepository;
        this.ambulanceRepository = ambulanceRepository;
        this.emergencyRepository = emergencyRepository;
        this.systemReadiness = systemReadiness;
        this.eventPublisher = eventPublisher;
        this.metrics = metrics;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void recoverSystemState() {

        systemReadiness.markNotReady(); // 🔒 BLOCK system first

        metrics.startupRecovery();


        // 1️⃣ Recover expired ASSIGNED assignments
        List<EmergencyAssignment> expired =
                assignmentRepository.findByStatusAndResponseDeadlineBefore(
                        EmergencyAssignmentStatus.ASSIGNED,
                        LocalDateTime.now()
                );

        for (EmergencyAssignment assignment : expired) {



            assignment.setStatus(EmergencyAssignmentStatus.REJECTED);
            assignment.setRejectedAt(LocalDateTime.now());
            assignmentRepository.save(assignment);

            Ambulance ambulance = assignment.getAmbulance();
            ambulance.setStatus(AmbulanceStatus.AVAILABLE);
            ambulanceRepository.save(ambulance);

            Emergency emergency = assignment.getEmergency();
            emergency.setStatus(EmergencyStatus.UNASSIGNED);
            emergency.setStatusUpdatedAt(LocalDateTime.now());
            emergencyRepository.save(emergency);

            // 📣 Per-emergency recovery event
            eventPublisher.publish(
                    new EmergencyEvent(
                            emergency.getId(),
                            "RECOVERY_ASSIGNMENT_EXPIRED",
                            "Expired assignment recovered on startup"
                    )
            );
        }

        // 2️⃣ Recover BUSY ambulances without active assignments
        List<Ambulance> busy = ambulanceRepository.findByStatus(AmbulanceStatus.BUSY);

        for (Ambulance ambulance : busy) {
            boolean hasActive =
                    assignmentRepository
                            .findByEmergencyId(ambulance.getId())
                            .stream()
                            .anyMatch(a ->
                                    a.getStatus() == EmergencyAssignmentStatus.ASSIGNED ||
                                            a.getStatus() == EmergencyAssignmentStatus.ACCEPTED
                            );

            if (!hasActive) {
                ambulance.setStatus(AmbulanceStatus.AVAILABLE);
                ambulanceRepository.save(ambulance);
            }
        }

        // ✅ System is now consistent
        systemReadiness.markReady();
    }
}

