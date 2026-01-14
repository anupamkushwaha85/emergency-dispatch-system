package com.hackathon.emergency108.event;

import com.hackathon.emergency108.entity.DomainEventEntity;
import com.hackathon.emergency108.repository.DomainEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DomainEventStore {

    private final DomainEventRepository repository;

    public DomainEventStore(DomainEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void persist(DomainEvent event) {

        DomainEventEntity entity =
                new DomainEventEntity(
                        event.eventType(),
                        event.aggregateType(),
                        event.aggregateId(),
                        event.message(),
                        event.occurredAt()
                );

        repository.save(entity);
    }
}
