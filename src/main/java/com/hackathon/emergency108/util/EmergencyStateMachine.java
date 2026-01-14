package com.hackathon.emergency108.util;

import com.hackathon.emergency108.entity.EmergencyStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class EmergencyStateMachine {

    private static final Map<EmergencyStatus, Set<EmergencyStatus>> ALLOWED_TRANSITIONS =
            new EnumMap<>(EmergencyStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(
                EmergencyStatus.CREATED,
                EnumSet.of(EmergencyStatus.IN_PROGRESS)
        );

        ALLOWED_TRANSITIONS.put(
                EmergencyStatus.IN_PROGRESS,
                EnumSet.of(EmergencyStatus.DISPATCHED, EmergencyStatus.UNASSIGNED)
        );

        ALLOWED_TRANSITIONS.put(
                EmergencyStatus.DISPATCHED,
                EnumSet.of(EmergencyStatus.COMPLETED, EmergencyStatus.UNASSIGNED)
        );

        ALLOWED_TRANSITIONS.put(
                EmergencyStatus.UNASSIGNED,
                EnumSet.of(EmergencyStatus.IN_PROGRESS)
        );

        ALLOWED_TRANSITIONS.put(
                EmergencyStatus.COMPLETED,
                EnumSet.noneOf(EmergencyStatus.class)
        );
    }

    private EmergencyStateMachine() {}

    public static boolean canTransition(
            EmergencyStatus from,
            EmergencyStatus to
    ) {
        return ALLOWED_TRANSITIONS
                .getOrDefault(from, EnumSet.noneOf(EmergencyStatus.class))
                .contains(to);
    }
}
