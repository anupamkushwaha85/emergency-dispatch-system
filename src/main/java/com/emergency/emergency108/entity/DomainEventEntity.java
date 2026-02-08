package com.emergency.emergency108.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "domain_events")
public class DomainEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String aggregateType;

    private Long aggregateId;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    protected DomainEventEntity() {
        // JPA
    }

    public DomainEventEntity(
            String eventType,
            String aggregateType,
            Long aggregateId,
            String message,
            LocalDateTime occurredAt
    ) {
        this.eventType = eventType;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.message = message;
        this.occurredAt = occurredAt;
    }
}
