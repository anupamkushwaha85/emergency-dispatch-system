package com.emergency.emergency108.event;

import java.time.LocalDateTime;

public interface DomainEvent {

    /* Core identity */
    String type();

    /* When it happened */
    LocalDateTime occurredAt();

    /* Human readable description */
    String description();

    /* -------- DEFAULT METADATA (safe defaults) -------- */

    default String eventType() {
        return type();
    }

    default String aggregateType() {
        return "UNKNOWN";
    }

    default Long aggregateId() {
        return null;
    }

    default String message() {
        return description();
    }
}
