package com.hackathon.emergency108.repository;

import com.hackathon.emergency108.entity.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository
        extends JpaRepository<AuditEvent, Long> {
}
