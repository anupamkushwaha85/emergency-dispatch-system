package com.emergency.emergency108.service;

import com.emergency.emergency108.dto.EmergencyTimelineEvent;
import com.emergency.emergency108.entity.*;
import com.emergency.emergency108.repository.EmergencyAssignmentRepository;
import com.emergency.emergency108.repository.EmergencyRepository;
import org.springframework.stereotype.Service;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class EmergencyTimelineService {

    private final EmergencyRepository emergencyRepository;
    private final EmergencyAssignmentRepository assignmentRepository;

    public EmergencyTimelineService(
            EmergencyRepository emergencyRepository,
            EmergencyAssignmentRepository assignmentRepository) {
        this.emergencyRepository = emergencyRepository;
        this.assignmentRepository = assignmentRepository;
    }

    public List<EmergencyTimelineEvent> getTimeline(Long emergencyId) {

        Emergency emergency = emergencyRepository.findById(emergencyId)
                .orElseThrow(() -> new RuntimeException("Emergency not found"));

        List<EmergencyTimelineEvent> timeline = new ArrayList<>();

        // CREATED
        timeline.add(new EmergencyTimelineEvent(
                "CREATED",
                emergency.getCreatedAt(),
                "Emergency created"
        ));


        // ASSIGNMENTS
        assignmentRepository
                .findByEmergencyId(emergencyId)
                .forEach(a -> {
                    timeline.add(new EmergencyTimelineEvent(
                            "ASSIGNED",
                            a.getAssignedAt(),
                            "Ambulance " + a.getAmbulance().getCode()
                    ));

                    if (a.getAcceptedAt() != null) {
                        timeline.add(new EmergencyTimelineEvent(
                                "ACCEPTED",
                                a.getAcceptedAt(),
                                "Driver accepted assignment"
                        ));
                    }

                    if (a.getRejectedAt() != null) {
                        timeline.add(new EmergencyTimelineEvent(
                                "REJECTED",
                                a.getRejectedAt(),
                                "Driver rejected / timeout"
                        ));
                    }

                    if (a.getCompletedAt() != null) {
                        timeline.add(new EmergencyTimelineEvent(
                                "COMPLETED",
                                a.getCompletedAt(),
                                "Emergency completed"
                        ));
                    }



                });

        // 3️⃣ FINAL EMERGENCY STATE
        if (emergency.getStatus() == EmergencyStatus.UNASSIGNED) {
            timeline.add(new EmergencyTimelineEvent(
                    "UNASSIGNED",
                    emergency.getStatusUpdatedAt(),
                    "All ambulances rejected or unavailable"
            ));
        }




        timeline.sort(Comparator.comparing(EmergencyTimelineEvent::getTime));
        return timeline;
    }
}
