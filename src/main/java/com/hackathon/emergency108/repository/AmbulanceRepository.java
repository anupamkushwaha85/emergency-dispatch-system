package com.hackathon.emergency108.repository;

import com.hackathon.emergency108.entity.Ambulance;
import com.hackathon.emergency108.entity.AmbulanceStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AmbulanceRepository extends JpaRepository<Ambulance, Long> {

    List<Ambulance> findByStatus(AmbulanceStatus status);

    /**
     * Find ambulances available for dispatch.
     * 
     * PRODUCTION-GRADE LOGIC: Only returns ambulances that:
     * 1. Have status = AVAILABLE
     * 2. Have an ONLINE verified driver operating them
     * 3. Driver has sent GPS heartbeat within last 30 seconds (not stale)
     * 4. Driver is not blocked
     * 
     * Uses pessimistic write lock to prevent concurrent assignments.
     * 
     * CRITICAL: The heartbeat check ensures we don't assign emergencies to drivers
     * whose app crashed, phone died, or lost network connection.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT a FROM Ambulance a
        INNER JOIN DriverSession ds ON a.id = ds.ambulanceId
        INNER JOIN User u ON ds.driverId = u.id
        WHERE a.status = 'AVAILABLE'
          AND ds.status = 'ONLINE'
          AND ds.sessionEndTime IS NULL
          AND ds.lastHeartbeat IS NOT NULL
          AND ds.lastHeartbeat > CURRENT_TIMESTAMP - 30 SECOND
          AND u.role = 'DRIVER'
          AND u.driverVerificationStatus = 'VERIFIED'
          AND u.blocked = false
        ORDER BY a.id
    """)
    List<Ambulance> findAvailableForUpdate();

}

