package com.hackathon.emergency108.repository;

import com.hackathon.emergency108.entity.DriverSession;
import com.hackathon.emergency108.entity.DriverSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DriverSessionRepository extends JpaRepository<DriverSession, Long> {

    /**
     * Find active session for a driver (ONLINE or ON_TRIP)
     */
    @Query("SELECT ds FROM DriverSession ds WHERE ds.driverId = :driverId " +
           "AND ds.status IN ('ONLINE', 'ON_TRIP') " +
           "AND ds.sessionEndTime IS NULL")
    Optional<DriverSession> findActiveSessionByDriverId(@Param("driverId") Long driverId);

    /**
     * Find active session for an ambulance (ONLINE or ON_TRIP)
     */
    @Query("SELECT ds FROM DriverSession ds WHERE ds.ambulanceId = :ambulanceId " +
           "AND ds.status IN ('ONLINE', 'ON_TRIP') " +
           "AND ds.sessionEndTime IS NULL")
    Optional<DriverSession> findActiveSessionByAmbulanceId(@Param("ambulanceId") Long ambulanceId);

    /**
     * Find all ONLINE drivers (available for assignment)
     * Must be VERIFIED and have ONLINE session
     */
    @Query("SELECT ds FROM DriverSession ds, User u " +
           "WHERE ds.driverId = u.id " +
           "AND ds.status = 'ONLINE' " +
           "AND ds.sessionEndTime IS NULL " +
           "AND u.driverVerificationStatus = 'VERIFIED' " +
           "ORDER BY ds.sessionStartTime ASC")
    List<DriverSession> findAllOnlineDrivers();

    /**
     * Find session by driver and ambulance (for validation)
     */
    @Query("SELECT ds FROM DriverSession ds WHERE ds.driverId = :driverId " +
           "AND ds.ambulanceId = :ambulanceId " +
           "AND ds.status IN ('ONLINE', 'ON_TRIP') " +
           "AND ds.sessionEndTime IS NULL")
    Optional<DriverSession> findActiveSessionByDriverAndAmbulance(
        @Param("driverId") Long driverId,
        @Param("ambulanceId") Long ambulanceId
    );

    /**
     * Find stale sessions (for cleanup job)
     * Sessions older than 24 hours that are still marked active
     */
    @Query("SELECT ds FROM DriverSession ds WHERE ds.status IN ('ONLINE', 'ON_TRIP') " +
           "AND ds.sessionStartTime < :cutoffTime " +
           "AND ds.sessionEndTime IS NULL")
    List<DriverSession> findStaleSessions(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Count active sessions for a driver
     */
    @Query("SELECT COUNT(ds) FROM DriverSession ds WHERE ds.driverId = :driverId " +
           "AND ds.status IN ('ONLINE', 'ON_TRIP') " +
           "AND ds.sessionEndTime IS NULL")
    long countActiveSessionsByDriverId(@Param("driverId") Long driverId);

    /**
     * Count active sessions for an ambulance
     */
    @Query("SELECT COUNT(ds) FROM DriverSession ds WHERE ds.ambulanceId = :ambulanceId " +
           "AND ds.status IN ('ONLINE', 'ON_TRIP') " +
           "AND ds.sessionEndTime IS NULL")
    long countActiveSessionsByAmbulanceId(@Param("ambulanceId") Long ambulanceId);

    /**
     * Find all sessions for a driver (history)
     */
    @Query("SELECT ds FROM DriverSession ds WHERE ds.driverId = :driverId " +
           "ORDER BY ds.sessionStartTime DESC")
    List<DriverSession> findAllByDriverId(@Param("driverId") Long driverId);

    /**
     * Find all active sessions (ONLINE or ON_TRIP) for stale detection
     */
    @Query("SELECT ds FROM DriverSession ds WHERE ds.status IN ('ONLINE', 'ON_TRIP') " +
           "AND ds.sessionEndTime IS NULL")
    List<DriverSession> findActiveSessions();
}
