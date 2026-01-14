package com.hackathon.emergency108.event;

import java.time.LocalDateTime;

public class EmergencyEvent implements DomainEvent {

    private final Long emergencyId;
    private final String type;
    private final String description;
    private final LocalDateTime occurredAt = LocalDateTime.now();

    public EmergencyEvent(
            Long emergencyId,
            String type,
            String description
    ) {
        this.emergencyId = emergencyId;
        this.type = type;
        this.description = description;
    }

    public Long getEmergencyId() {
        return emergencyId;
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
        return "EMERGENCY";
    }

}
