package com.emergency.emergency108.repository;

import com.emergency.emergency108.entity.EmergencyAssignment;
import com.emergency.emergency108.entity.EmergencyAssignmentStatus;
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

        Optional<EmergencyAssignment> findTopByEmergencyIdOrderByAssignedAtDesc(Long emergencyId);

        List<EmergencyAssignment> findByStatusAndResponseDeadlineBefore(
                        EmergencyAssignmentStatus status,
                        LocalDateTime time);

        Optional<EmergencyAssignment> findByEmergencyIdAndStatus(
                        Long emergencyId,
                        EmergencyAssignmentStatus status);

        List<EmergencyAssignment> findByEmergencyId(Long emergencyId);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("""
                        select a from EmergencyAssignment a
                        where a.emergency.id = :emergencyId
                        and a.status = 'ASSIGNED'
                        """)
        Optional<EmergencyAssignment> findActiveAssignmentForUpdate(
                        @Param("emergencyId") Long emergencyId);

        /**
         * Find assignment by emergency ID, driver ID, and status.
         * Used for authorization checks.
         */
        Optional<EmergencyAssignment> findByEmergencyIdAndDriverIdAndStatus(
                        Long emergencyId,
                        Long driverId,
                        EmergencyAssignmentStatus status);

        /**
         * Find assignment by driver ID and status only.
         * Used to get current assignment for driver.
         */
        Optional<EmergencyAssignment> findByDriverIdAndStatus(
                        Long driverId,
                        EmergencyAssignmentStatus status);

        /**
         * Find all assignments with timeout deadline passed.
         * Used by scheduled job to handle driver timeouts.
         */
        @Query("""
                        select a from EmergencyAssignment a
                        where a.status = 'ASSIGNED'
                        and a.responseDeadline < :now
                        """)
        List<EmergencyAssignment> findTimedOutAssignments(@Param("now") LocalDateTime now);

        @Query("SELECT a.driverId FROM EmergencyAssignment a WHERE a.emergency.id = :emergencyId AND a.status = 'REJECTED'")
        List<Long> findRejectedDriverIdsByEmergencyId(@Param("emergencyId") Long emergencyId);

}
