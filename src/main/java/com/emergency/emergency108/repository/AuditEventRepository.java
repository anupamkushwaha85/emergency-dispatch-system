package com.emergency.emergency108.repository;

import com.emergency.emergency108.entity.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author anupam kushwaha
 */
public interface AuditEventRepository
        extends JpaRepository<AuditEvent, Long> {
}
