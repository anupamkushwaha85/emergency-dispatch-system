package com.hackathon.emergency108.event;

import com.hackathon.emergency108.resilience.DomainSafety;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DomainEventPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(DomainEventPublisher.class);

    private final AuditEventStore store;

    public DomainEventPublisher(AuditEventStore store) {
        this.store = store;
    }

    public void publish(DomainEvent event) {

        DomainSafety.runSafely(
                "AUDIT_EVENT_PERSIST",
                () -> store.persist(event)
        );
    }
}

