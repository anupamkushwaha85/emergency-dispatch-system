package com.hackathon.emergency108.util;

import com.hackathon.emergency108.entity.EmergencyAssignmentStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class EmergencyAssignmentStateMachine {

    private static final Map<EmergencyAssignmentStatus, Set<EmergencyAssignmentStatus>> ALLOWED;

    static {
        ALLOWED = new EnumMap<>(EmergencyAssignmentStatus.class);

        // ASSIGNED → ACCEPTED / REJECTED
        ALLOWED.put(
                EmergencyAssignmentStatus.ASSIGNED,
                EnumSet.of(
                        EmergencyAssignmentStatus.ACCEPTED,
                        EmergencyAssignmentStatus.REJECTED
                )
        );

        // ACCEPTED → COMPLETED
        ALLOWED.put(
                EmergencyAssignmentStatus.ACCEPTED,
                EnumSet.of(EmergencyAssignmentStatus.COMPLETED)
        );

        // REJECTED → (terminal)
        ALLOWED.put(
                EmergencyAssignmentStatus.REJECTED,
                EnumSet.noneOf(EmergencyAssignmentStatus.class)
        );

        // COMPLETED → (terminal)
        ALLOWED.put(
                EmergencyAssignmentStatus.COMPLETED,
                EnumSet.noneOf(EmergencyAssignmentStatus.class)
        );
    }

    private EmergencyAssignmentStateMachine() {}

    public static boolean canTransition(
            EmergencyAssignmentStatus current,
            EmergencyAssignmentStatus next
    ) {
        return ALLOWED
                .getOrDefault(current, EnumSet.noneOf(EmergencyAssignmentStatus.class))
                .contains(next);
    }
}
