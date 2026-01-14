package com.hackathon.emergency108.event;

import com.hackathon.emergency108.entity.AuditEvent;
import com.hackathon.emergency108.repository.AuditEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditEventStore {

    private final AuditEventRepository repository;

    public AuditEventStore(AuditEventRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persist(DomainEvent event) {

        AuditEvent audit = new AuditEvent(
                event.eventType(),
                event.aggregateType(),
                event.aggregateId(),
                event.message()
        );

        repository.save(audit);
    }
}
