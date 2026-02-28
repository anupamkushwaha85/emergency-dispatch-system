package com.emergency.emergency108.repository;

import com.emergency.emergency108.entity.Ambulance;
import com.emergency.emergency108.entity.AmbulanceStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AmbulanceRepository extends JpaRepository<Ambulance, Long> {

  List<Ambulance> findByStatus(AmbulanceStatus status);

  java.util.Optional<Ambulance> findByCode(String code);

  /**
   * Find the ambulance assigned to this driver.
   *
   * Checks two sources in priority order (OR):
   * 1. driver_id column — set by admin via PUT /api/ambulances/{id}/assign
   * 2. Active DriverSession JOIN — fallback for ambulances assigned before
   *    the driver_id column was introduced (backward-compatible migration path)
   *
   * This ensures both pre-migration and post-migration ambulances are found.
   */
  @Query("SELECT a FROM Ambulance a WHERE a.driverId = :driverId " +
         "OR EXISTS (SELECT ds FROM DriverSession ds WHERE ds.ambulanceId = a.id " +
         "AND ds.driverId = :driverId AND ds.sessionEndTime IS NULL)")
  java.util.Optional<Ambulance> findByDriverId(@Param("driverId") Long driverId);

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
