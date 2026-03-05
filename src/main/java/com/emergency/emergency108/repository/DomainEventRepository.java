package com.emergency.emergency108.repository;

import com.emergency.emergency108.entity.DomainEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author anupam kushwaha
 */
public interface DomainEventRepository
        extends JpaRepository<DomainEventEntity, Long> {
}
