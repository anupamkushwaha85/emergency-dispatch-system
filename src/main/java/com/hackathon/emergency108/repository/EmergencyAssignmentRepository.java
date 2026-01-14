package com.hackathon.emergency108.repository;

import com.hackathon.emergency108.entity.EmergencyAssignment;
import com.hackathon.emergency108.entity.EmergencyAssignmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EmergencyAssignmentRepository
        extends JpaRepository<EmergencyAssignment, Long> {
    boolean existsByEmergencyId(Long emergencyId);

    Optional<EmergencyAssignment>
    findTopByEmergencyIdOrderByAssignedAtDesc(Long emergencyId);

    List<EmergencyAssignment>
    findByStatusAndResponseDeadlineBefore(
            EmergencyAssignmentStatus status,
            LocalDateTime time
    );
    Optional<EmergencyAssignment>
    findByEmergencyIdAndStatus(
            Long emergencyId,
            EmergencyAssignmentStatus status
    );

    List<EmergencyAssignment> findByEmergencyId(Long emergencyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
select a from EmergencyAssignment a
where a.emergency.id = :emergencyId
and a.status = 'ASSIGNED'
""")
    Optional<EmergencyAssignment> findActiveAssignmentForUpdate(
            @Param("emergencyId") Long emergencyId
    );



}
