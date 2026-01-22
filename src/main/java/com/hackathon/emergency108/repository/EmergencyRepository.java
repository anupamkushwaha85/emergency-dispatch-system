package com.hackathon.emergency108.repository;

import com.hackathon.emergency108.entity.Emergency;
import com.hackathon.emergency108.entity.EmergencyStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;


public interface EmergencyRepository extends JpaRepository<Emergency, Long> {

    List<Emergency> findByStatus(EmergencyStatus status);

    /**
     * Find all CREATED emergencies that have passed their confirmation deadline.
     * Used by scheduled job to auto-dispatch unconfirmed emergencies.
     */
    @Query("""
select e from Emergency e
where e.status = 'CREATED'
and e.confirmationDeadline < :now
""")
    List<Emergency> findUnconfirmedEmergencies(@Param("now") LocalDateTime now);

}

