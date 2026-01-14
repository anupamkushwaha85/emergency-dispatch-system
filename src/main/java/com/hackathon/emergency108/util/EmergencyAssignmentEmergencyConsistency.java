package com.hackathon.emergency108.util;

import com.hackathon.emergency108.entity.EmergencyAssignmentStatus;
import com.hackathon.emergency108.entity.EmergencyStatus;

public final class EmergencyAssignmentEmergencyConsistency {

    private EmergencyAssignmentEmergencyConsistency() {}

    public static void validate(
            EmergencyAssignmentStatus assignmentStatus,
            EmergencyStatus emergencyStatus
    ) {

        switch (assignmentStatus) {

            case ASSIGNED -> {
                if (emergencyStatus != EmergencyStatus.IN_PROGRESS) {
                    throw new InvalidAssignmentStateException(
                            "ASSIGNED assignment requires emergency IN_PROGRESS, found " + emergencyStatus
                    );
                }
            }

            case ACCEPTED -> {
                if (emergencyStatus != EmergencyStatus.DISPATCHED) {
                    throw new InvalidAssignmentStateException(
                            "ACCEPTED assignment requires emergency DISPATCHED, found " + emergencyStatus
                    );
                }
            }

            case COMPLETED -> {
                if (emergencyStatus != EmergencyStatus.COMPLETED) {
                    throw new InvalidAssignmentStateException(
                            "COMPLETED assignment requires emergency COMPLETED, found " + emergencyStatus
                    );
                }
            }

            case REJECTED -> {
                // REJECTED is valid for IN_PROGRESS or UNASSIGNED
                if (emergencyStatus != EmergencyStatus.IN_PROGRESS &&
                        emergencyStatus != EmergencyStatus.UNASSIGNED) {

                    throw new InvalidAssignmentStateException(
                            "REJECTED assignment incompatible with emergency " + emergencyStatus
                    );
                }
            }
        }
    }
}
