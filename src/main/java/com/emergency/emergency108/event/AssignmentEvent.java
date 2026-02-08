package com.emergency.emergency108.event;

import java.time.LocalDateTime;

public class AssignmentEvent implements DomainEvent {

    private final Long emergencyId;
    private final Long ambulanceId;
    private final String type;
    private final String description;
    private final LocalDateTime occurredAt = LocalDateTime.now();

    public AssignmentEvent(
            Long emergencyId,
            Long ambulanceId,
            String type,
            String description
    ) {
        this.emergencyId = emergencyId;
        this.ambulanceId = ambulanceId;
        this.type = type;
        this.description = description;
    }

    public Long getEmergencyId() {
        return emergencyId;
    }

    public Long getAmbulanceId() {
        return ambulanceId;
    }

    @Override
    public String type() {
        return type;
    }

    @Override
    public LocalDateTime occurredAt() {
        return occurredAt;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public String aggregateType() {
        return "ASSIGNMENT";
    }

}
