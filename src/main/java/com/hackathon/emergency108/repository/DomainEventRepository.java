package com.hackathon.emergency108.repository;

import com.hackathon.emergency108.entity.DomainEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DomainEventRepository
        extends JpaRepository<DomainEventEntity, Long> {
}
