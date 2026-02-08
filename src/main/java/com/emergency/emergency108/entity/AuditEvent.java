package com.emergency.emergency108.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "audit_events",
        indexes = {
                @Index(name = "idx_audit_type", columnList = "eventType"),
                @Index(name = "idx_audit_time", columnList = "occurredAt")
        }
)
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String aggregateType;

    @Column(nullable = false)
    private Long aggregateId;

    @Column(nullable = false, length = 2000)
    private String message;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    protected AuditEvent() {}

    public AuditEvent(
            String eventType,
            String aggregateType,
            Long aggregateId,
            String message
    ) {
        this.eventType = eventType;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.message = message;
        this.occurredAt = LocalDateTime.now();
    }

    // getters only (IMMUTABLE)
    public Long getId() { return id; }
    public String getEventType() { return eventType; }
    public String getAggregateType() { return aggregateType; }
    public Long getAggregateId() { return aggregateId; }
    public String getMessage() { return message; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}
