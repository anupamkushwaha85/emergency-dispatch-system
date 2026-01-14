package com.hackathon.emergency108.service;

import com.hackathon.emergency108.entity.Ambulance;
import com.hackathon.emergency108.entity.AmbulanceStatus;
import com.hackathon.emergency108.entity.EmergencyAssignment;
import com.hackathon.emergency108.entity.EmergencyAssignmentStatus;
import com.hackathon.emergency108.repository.AmbulanceRepository;
import com.hackathon.emergency108.repository.EmergencyAssignmentRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class EmergencyAutoReassignService {

    private final EmergencyAssignmentRepository assignmentRepository;
    private final AmbulanceRepository ambulanceRepository;
    private final EmergencyAssignmentService assignmentService;



    public EmergencyAutoReassignService(
            EmergencyAssignmentRepository assignmentRepository,
            AmbulanceRepository ambulanceRepository,
            EmergencyAssignmentService assignmentService ) {
        this.assignmentService = assignmentService;
        this.assignmentRepository = assignmentRepository;
        this.ambulanceRepository = ambulanceRepository;
    }

    @Transactional
    public void handleTimeouts() {

        List<EmergencyAssignment> expired =
                assignmentRepository.findByStatusAndResponseDeadlineBefore(
                        EmergencyAssignmentStatus.ASSIGNED,
                        LocalDateTime.now()
                );

        for (EmergencyAssignment assignment : expired) {

            // 1️⃣ Mark assignment rejected
            assignment.setStatus(EmergencyAssignmentStatus.REJECTED);
            assignmentRepository.save(assignment);

            // 2️⃣ Free ambulance
            Ambulance amb = assignment.getAmbulance();
            amb.setStatus(AmbulanceStatus.AVAILABLE);
            ambulanceRepository.save(amb);

            // 3️⃣ Retry assignment (CORRECT PLACE)
            assignmentService.rejectAndRetry(
                    assignment.getEmergency(),
                    amb
            );
        }
    }
}
