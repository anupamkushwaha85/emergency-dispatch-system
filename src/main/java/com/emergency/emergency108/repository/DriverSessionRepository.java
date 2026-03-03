package com.emergency.emergency108.repository;

import com.emergency.emergency108.entity.DriverSession;
import com.emergency.emergency108.entity.DriverSessionStatus;
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
        * Find a recently disconnected session for a driver that is eligible for reconnect reactivation.
        * Criteria: status=OFFLINE, sessionEndTime IS NULL (disconnected by network, not explicit endShift),
        * and session started after cutoffTime (within the reactivation window).
        */
       @Query("SELECT ds FROM DriverSession ds WHERE ds.driverId = :driverId " +
                     "AND ds.status = 'OFFLINE' " +
                     "AND ds.sessionEndTime IS NULL " +
                     "AND ds.sessionStartTime > :cutoffTime " +
                     "ORDER BY ds.sessionStartTime DESC")
       Optional<DriverSession> findRecentlyDisconnectedSession(
                     @Param("driverId") Long driverId,
                     @Param("cutoffTime") LocalDateTime cutoffTime);

       /**
        * Find ALL orphan OFFLINE sessions for a driver that were left by STOMP disconnect
        * (sessionEndTime IS NULL = disconnected by network, not explicit endShift).
        * Used by startShift to clean them up before creating a new session.
        */
       @Query("SELECT ds FROM DriverSession ds WHERE ds.driverId = :driverId " +
                     "AND ds.status = 'OFFLINE' " +
                     "AND ds.sessionEndTime IS NULL")
       List<DriverSession> findOrphanDisconnectedSessions(@Param("driverId") Long driverId);

       /**
        * Find eligible ONLINE drivers with recent heartbeat or recent session start
        * Must be VERIFIED, have ONLINE session, and be active within the cutoff time.
        */
       @Query("SELECT ds FROM DriverSession ds, User u " +
                     "WHERE ds.driverId = u.id " +
                     "AND ds.status = 'ONLINE' " +
                     "AND ds.sessionEndTime IS NULL " +
                     "AND u.driverVerificationStatus = 'VERIFIED' " +
                     "AND (ds.lastHeartbeat >= :cutoffTime OR (ds.lastHeartbeat IS NULL AND ds.sessionStartTime >= :cutoffTime)) "
                     +
                     "ORDER BY ds.sessionStartTime ASC")
       List<DriverSession> findEligibleOnlineDrivers(@Param("cutoffTime") LocalDateTime cutoffTime);

       /**
        * Find drivers available for dispatch.
        * Accepts ONLINE sessions AND OFFLINE sessions with a recent REST heartbeat
        * (covers Cloudflare/proxy WebSocket timeout scenarios where the STOMP connection
        * was dropped and the session flipped to OFFLINE, but the driver's app is still
        * alive and sending REST PUT /driver/location heartbeats).
        * Window: 2 minutes — generous enough to survive brief STOMP reconnects.
        */
       @Query("SELECT ds FROM DriverSession ds, User u " +
                     "WHERE ds.driverId = u.id " +
                     "AND ds.sessionEndTime IS NULL " +
                     "AND u.driverVerificationStatus = 'VERIFIED' " +
                     "AND (" +
                     "  ds.status = 'ONLINE' " +
                     "  OR (ds.status = 'OFFLINE' AND ds.lastHeartbeat IS NOT NULL AND ds.lastHeartbeat >= :heartbeatCutoff) " +
                     ") " +
                     "ORDER BY ds.sessionStartTime ASC")
       List<DriverSession> findAvailableDriversForDispatch(@Param("heartbeatCutoff") LocalDateTime heartbeatCutoff);

       /**
        * Find session by driver and ambulance (for validation)
        */
       @Query("SELECT ds FROM DriverSession ds WHERE ds.driverId = :driverId " +
                     "AND ds.ambulanceId = :ambulanceId " +
                     "AND ds.status IN ('ONLINE', 'ON_TRIP') " +
                     "AND ds.sessionEndTime IS NULL")
       Optional<DriverSession> findActiveSessionByDriverAndAmbulance(
                     @Param("driverId") Long driverId,
                     @Param("ambulanceId") Long ambulanceId);

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
