package com.emergency.emergency108.service;

import com.emergency.emergency108.dto.DriverStatusDTO;
import com.emergency.emergency108.dto.LocationUpdateDTO;
import com.emergency.emergency108.entity.*;
import com.emergency.emergency108.metrics.DomainMetrics;
import com.emergency.emergency108.repository.AmbulanceRepository;
import com.emergency.emergency108.repository.DriverSessionRepository;
import com.emergency.emergency108.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Production-grade driver session management service.
 * Handles driver shifts, location updates, and session lifecycle.
 */
/**
 * Service implementation for DriverSession operations.
 *
 * @author anupam kushwaha
 */
@Service
public class DriverSessionService {

    private static final Logger log = LoggerFactory.getLogger(DriverSessionService.class);

    private static final int MAX_SESSION_DURATION_HOURS = 24;
    private static final int SESSION_CLEANUP_HOURS = 24;

    private final DriverSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final AmbulanceRepository ambulanceRepository;
    private final DomainMetrics metrics;
    private final SimpMessagingTemplate messagingTemplate;
    private final TrackingBroadcastService trackingBroadcastService;

    public DriverSessionService(
            DriverSessionRepository sessionRepository,
            UserRepository userRepository,
            AmbulanceRepository ambulanceRepository,
            DomainMetrics metrics,
            SimpMessagingTemplate messagingTemplate,
            TrackingBroadcastService trackingBroadcastService) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.ambulanceRepository = ambulanceRepository;
        this.metrics = metrics;
        this.messagingTemplate = messagingTemplate;
        this.trackingBroadcastService = trackingBroadcastService;
    }

    /**
     * Start a new driver shift with specified ambulance.
     * 
     * Validations:
     * - Driver must be VERIFIED
     * - Driver cannot have another active session
     * - Driver cannot be ON_TRIP (invariant check)
     * - Ambulance cannot be in use by another driver
     * - Ambulance must exist and be AVAILABLE
     */
    @Transactional
    public DriverSession startShift(Long driverId, Long ambulanceId) {
        log.info("Driver {} attempting to start shift with ambulance {}", driverId, ambulanceId);

        // 1️⃣ Validate driver exists and is verified
        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found: " + driverId));

        if (driver.getRole() != UserRole.DRIVER) {
            throw new IllegalStateException("User is not a DRIVER");
        }

        if (driver.getDriverVerificationStatus() != DriverVerificationStatus.VERIFIED) {
            throw new IllegalStateException(
                    "Driver not verified. Current status: " + driver.getDriverVerificationStatus());
        }

        if (driver.isBlocked()) {
            throw new IllegalStateException("Driver account is blocked");
        }

        // 2️⃣ Clean up orphan OFFLINE sessions (disconnect-orphans with no sessionEndTime)
        //    These are created by markDriverOfflineFromDisconnect which does NOT set sessionEndTime.
        //    If uncleaned, reactivateIfDisconnected would reactivate the old one AFTER the new
        //    session is created → 2 ONLINE sessions → NonUniqueResultException crashes.
        List<DriverSession> orphans = sessionRepository.findOrphanDisconnectedSessions(driverId);
        if (!orphans.isEmpty()) {
            log.info("Cleaning {} orphan disconnected sessions for driver {} before new shift",
                    orphans.size(), driverId);
            for (DriverSession orphan : orphans) {
                orphan.setSessionEndTime(LocalDateTime.now());
                sessionRepository.save(orphan);
            }
        }

        // 3️⃣ INVARIANT CHECK: Driver cannot start shift if already ONLINE or ON_TRIP
        Optional<DriverSession> existingDriverSession = sessionRepository.findActiveSessionByDriverId(driverId);

        if (existingDriverSession.isPresent()) {
            DriverSession existing = existingDriverSession.get();

            // CRITICAL INVARIANT: Cannot start shift while ON_TRIP
            if (existing.getStatus() == DriverSessionStatus.ON_TRIP) {
                throw new IllegalStateException(
                        String.format(
                                "INVARIANT VIOLATION: Driver is currently ON_TRIP (Session ID: %d, Ambulance: %d). " +
                                        "Complete the current emergency before starting a new shift.",
                                existing.getId(),
                                existing.getAmbulanceId()));
            }

            throw new IllegalStateException(
                    String.format(
                            "Driver already has an active session (ID: %d, Ambulance: %d, Status: %s). End current session first.",
                            existing.getId(),
                            existing.getAmbulanceId(),
                            existing.getStatus()));
        }

        // 3️⃣ Check if ambulance exists and is available
        Ambulance ambulance = ambulanceRepository.findById(ambulanceId)
                .orElseThrow(() -> new IllegalArgumentException("Ambulance not found: " + ambulanceId));

        if (ambulance.getStatus() != AmbulanceStatus.AVAILABLE) {
            throw new IllegalStateException(
                    "Ambulance is not available. Current status: " + ambulance.getStatus());
        }

        // 4️⃣ Check if ambulance is already being used by another driver
        Optional<DriverSession> existingAmbulanceSession = sessionRepository
                .findActiveSessionByAmbulanceId(ambulanceId);

        if (existingAmbulanceSession.isPresent()) {
            DriverSession existing = existingAmbulanceSession.get();
            throw new IllegalStateException(
                    String.format(
                            "Ambulance is already in use by driver %d (Session ID: %d, Status: %s)",
                            existing.getDriverId(),
                            existing.getId(),
                            existing.getStatus()));
        }

        // 5️⃣ Create new session
        DriverSession session = new DriverSession(driverId, ambulanceId);

        // Set initial location from ambulance's last known location
        if (ambulance.getLatitude() != null && ambulance.getLongitude() != null) {
            session.updateLocation(ambulance.getLatitude(), ambulance.getLongitude());
        }

        DriverSession savedSession = sessionRepository.save(session);

        metrics.driverShiftStarted();

        log.info("✅ Driver {} started shift with ambulance {} (Session ID: {})",
                driverId, ambulanceId, savedSession.getId());

        // Broadcast online status to admin dashboard
        broadcastDriverStatus(savedSession, driverId);

        return savedSession;
    }

    /**
     * End driver's current shift.
     * Driver must not be on an active trip.
     */
    @Transactional

    public void endShift(Long driverId) {
        log.info("Driver {} attempting to end shift", driverId);

        Optional<DriverSession> sessionOpt = sessionRepository.findActiveSessionByDriverId(driverId);

        if (sessionOpt.isEmpty()) {
            log.warn("⚠️ No active session found for driver {} when ending shift. Assuming already offline.", driverId);
            return; // Idempotent success
        }

        DriverSession session = sessionOpt.get();

        if (session.getStatus() == DriverSessionStatus.ON_TRIP) {
            throw new IllegalStateException(
                    "Cannot end shift while on an active trip. Complete the emergency first.");
        }

        try {
            session.endSession();
            sessionRepository.save(session);

            Duration shiftDuration = Duration.between(
                    session.getSessionStartTime(),
                    session.getSessionEndTime());

            metrics.driverShiftEnded();

            log.info("✅ Driver {} ended shift. Duration: {} hours, Emergencies handled: {}",
                    driverId,
                    shiftDuration.toHours(),
                    session.getEmergenciesHandled());

            // Broadcast offline status to admin dashboard
            broadcastDriverStatus(session, driverId);

        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("Optimistic lock failure ending shift for driver {}. Retrying...", driverId);
            throw new IllegalStateException("Session was modified by another process. Please try again.");
        }
    }

    /**
     * Update driver's current location during shift.
     * Also updates heartbeat timestamp to indicate driver is alive.
     * 
     * HEARTBEAT MECHANISM:
     * - Driver app calls this endpoint every 3-5 seconds automatically
     * - Updates both location AND heartbeat timestamp
     * - If heartbeat not updated for 30+ seconds, driver marked OFFLINE
     */
    @Transactional
    public void updateLocation(Long driverId, double lat, double lng) {
        DriverSession session = sessionRepository.findActiveSessionByDriverId(driverId)
                .orElseThrow(() -> new IllegalStateException("No active session found for driver " + driverId));

        // Update location
        session.updateLocation(lat, lng);

        // Update heartbeat timestamp (critical for stale detection)
        session.updateHeartbeat();

        metrics.heartbeatReceived();

        sessionRepository.save(session);

        // Also update ambulance location
        Ambulance ambulance = ambulanceRepository.findById(session.getAmbulanceId())
                .orElseThrow(() -> new IllegalStateException("Ambulance not found"));

        ambulance.updateLocation(lat, lng);
        ambulanceRepository.save(ambulance);

        log.debug("Updated location and heartbeat for driver {} at ({}, {})", driverId, lat, lng);

        // ── WebSocket broadcast ────────────────────────────────────────────────
        // Push location to all connected admin dashboards instantly.
        // This replaces the 10-second HTTP polling on the LiveMap admin page.
        LocationUpdateDTO locationUpdate = new LocationUpdateDTO(
                driverId,
                session.getAmbulanceId(),
                lat,
                lng,
                LocalDateTime.now());

        // Global feed — admin map subscribes here to track all ambulances
        messagingTemplate.convertAndSend("/topic/live-locations", locationUpdate);

        // Per-driver feed — for future use (e.g. user tracking their own ambulance)
        messagingTemplate.convertAndSend("/topic/driver/" + driverId, locationUpdate);

        // Patient tracking broadcast — replaces HTTP polling in Flutter patient app.
        // Fires only when driver is on active trip (ACCEPTED assignment); a no-op otherwise.
        trackingBroadcastService.broadcastForDriver(driverId, lat, lng);
        // ──────────────────────────────────────────────────────────────────────
    }

    /**
     * Mark driver as ON_TRIP when starting an emergency.
     * Called internally by assignment service.
     * 
     * TRANSACTION SAFETY: This method is atomic and validates state before
     * transition.
     */
    @Transactional
    public void markDriverOnTrip(Long driverId) {
        DriverSession session = sessionRepository.findActiveSessionByDriverId(driverId)
                .orElseThrow(() -> new IllegalStateException("No active session found for driver " + driverId));

        // Validate state transition is legal
        if (session.getStatus() != DriverSessionStatus.ONLINE) {
            throw new IllegalStateException(
                    String.format(
                            "INVARIANT VIOLATION: Cannot mark driver as ON_TRIP. Current status: %s (expected: ONLINE)",
                            session.getStatus()));
        }

        session.startTrip();
        sessionRepository.save(session);

        log.info("Driver {} marked as ON_TRIP (Session ID: {})", driverId, session.getId());

        // Broadcast ON_TRIP status to admin dashboard
        broadcastDriverStatus(session, driverId);
    }

    /**
     * Mark driver as back ONLINE after completing an emergency.
     * Called internally by assignment service.
     * 
     * TRANSACTION SAFETY: This method is atomic and validates state before
     * transition.
     */
    @Transactional
    public void markDriverOnline(Long driverId) {
        DriverSession session = sessionRepository.findActiveSessionByDriverId(driverId)
                .orElseThrow(() -> new IllegalStateException("No active session found for driver " + driverId));

        // Validate state transition is legal
        if (session.getStatus() != DriverSessionStatus.ON_TRIP) {
            log.warn("Attempted to mark driver {} as ONLINE but status is {} (expected: ON_TRIP)",
                    driverId, session.getStatus());
            // Allow transition even if already ONLINE (idempotent for retry scenarios)
            if (session.getStatus() == DriverSessionStatus.ONLINE) {
                log.debug("Driver {} already ONLINE, skipping transition", driverId);
                return;
            }
            throw new IllegalStateException(
                    String.format(
                            "INVARIANT VIOLATION: Cannot mark driver as ONLINE. Current status: %s (expected: ON_TRIP)",
                            session.getStatus()));
        }

        session.endTrip();
        sessionRepository.save(session);

        log.info("Driver {} marked as ONLINE (Session ID: {}, Total emergencies: {})",
                driverId, session.getId(), session.getEmergenciesHandled());

        // Broadcast back-ONLINE status to admin dashboard
        broadcastDriverStatus(session, driverId);
    }

    /**
     * Validate driver can accept an emergency assignment.
     * 
     * INVARIANT CHECKS:
     * - Driver must have an active session
     * - Driver must be ONLINE (not ON_TRIP or OFFLINE)
     * - Driver must be operating the specified ambulance
     * 
     * @throws IllegalStateException if driver cannot accept
     */
    @Transactional(readOnly = true)
    public void validateCanAcceptEmergency(Long driverId, Long ambulanceId) {
        // Heartbeat-tolerant: try ONLINE/ON_TRIP first, fall back to recent-heartbeat OFFLINE
        DriverSession session = getOrReactivateSession(driverId);
        if (session == null) {
            throw new IllegalStateException(
                    "Driver has no active session. Start a shift first.");
        }

        // INVARIANT: Driver must be ONLINE to accept new emergencies
        if (session.getStatus() != DriverSessionStatus.ONLINE) {
            throw new IllegalStateException(
                    String.format(
                            "INVARIANT VIOLATION: Driver cannot accept emergency while status is %s. " +
                                    "Driver must be ONLINE. Current session ID: %d",
                            session.getStatus(),
                            session.getId()));
        }

        // INVARIANT: Driver must be operating the assigned ambulance
        if (!session.getAmbulanceId().equals(ambulanceId)) {
            throw new IllegalStateException(
                    String.format(
                            "INVARIANT VIOLATION: Driver is operating ambulance %d but emergency is assigned to ambulance %d",
                            session.getAmbulanceId(),
                            ambulanceId));
        }

        log.debug("✅ Driver {} validated for emergency acceptance (Session: {}, Ambulance: {})",
                driverId, session.getId(), ambulanceId);
    }

    /**
     * Handle driver rejection of emergency assignment.
     * Driver status remains ONLINE after rejection.
     * 
     * TRANSACTION SAFETY: Validates driver is in correct state to reject.
     */
    @Transactional(readOnly = true)
    public void validateRejection(Long driverId, Long ambulanceId) {
        DriverSession session = sessionRepository.findActiveSessionByDriverId(driverId)
                .orElseThrow(() -> new IllegalStateException(
                        "Driver has no active session"));

        // Driver can reject from ONLINE status
        if (session.getStatus() != DriverSessionStatus.ONLINE) {
            throw new IllegalStateException(
                    String.format(
                            "Driver cannot reject emergency while status is %s (expected: ONLINE)",
                            session.getStatus()));
        }

        // Verify ambulance ownership
        if (!session.getAmbulanceId().equals(ambulanceId)) {
            throw new IllegalStateException(
                    String.format(
                            "Driver is operating ambulance %d but emergency is assigned to ambulance %d",
                            session.getAmbulanceId(),
                            ambulanceId));
        }

        log.debug("✅ Driver {} validated for emergency rejection (remains ONLINE)", driverId);
    }

    /**
     * Get driver's current active session.
     */
    @Transactional(readOnly = true)
    public Optional<DriverSession> getCurrentSession(Long driverId) {
        return sessionRepository.findActiveSessionByDriverId(driverId);
    }

    /**
     * Check if driver is currently online and available.
     */
    @Transactional(readOnly = true)
    public boolean isDriverOnline(Long driverId) {
        return sessionRepository.findActiveSessionByDriverId(driverId)
                // Driver is considered "online" for Flutter if ONLINE or ON_TRIP
                // (ON_TRIP = actively handling a mission — still needs tracking/location)
                .map(s -> s.getStatus() == DriverSessionStatus.ONLINE
                        || s.getStatus() == DriverSessionStatus.ON_TRIP)
                .orElse(false);
    }

    /**
     * Mark a driver OFFLINE because their STOMP WebSocket disconnected.
     *
     * Unlike {@link #endShift} this does NOT throw if the driver is ON_TRIP —
     * a network drop during a trip is a real scenario and we must still clean up
     * the session so dispatch does not see a phantom ONLINE driver.
     *
     * Called exclusively by {@link com.emergency.emergency108.config.WebSocketEventListener}.
     */
    @Transactional
    public void markDriverOfflineFromDisconnect(Long driverId) {
        Optional<DriverSession> sessionOpt = sessionRepository.findActiveSessionByDriverId(driverId);
        if (sessionOpt.isEmpty()) {
            // Already offline — idempotent
            return;
        }
        DriverSession session = sessionOpt.get();
        session.setStatus(DriverSessionStatus.OFFLINE);
        // NOTE: Do NOT set sessionEndTime here.
        // sessionEndTime=null means "disconnected by network drop, eligible for reactivation on reconnect".
        // sessionEndTime!=null means "explicitly ended shift" (set only by endShift or stale cleanup).
        sessionRepository.save(session);
        log.info("🔴 Driver {} session {} marked OFFLINE via STOMP disconnect (eligible for reconnect reactivation)", driverId, session.getId());
        broadcastDriverStatus(session, driverId);
    }

    /**
     * Reactivate a driver's session when their STOMP WebSocket reconnects.
     *
     * Looks for a recent OFFLINE session with null sessionEndTime (caused by a
     * network-drop disconnect). If found within the reactivation window, restores
     * status to ONLINE so dispatch can find the driver without requiring a manual
     * "start shift" flow from the Flutter app.
     */
    @Transactional
    public void reactivateIfDisconnected(Long driverId) {
        // Skip reactivation if there's already an active session (ONLINE or ON_TRIP).
        // This happens when startShift was called before STOMP reconnect — the fresh
        // session is already authoritative; reactivating the old orphan would create
        // a duplicate that causes NonUniqueResultException throughout the app.
        Optional<DriverSession> activeSession = sessionRepository.findActiveSessionByDriverId(driverId);
        if (activeSession.isPresent()) {
            log.debug("Driver {} already has an active session (status={}), skipping reactivation",
                    driverId, activeSession.get().getStatus());
            return;
        }

        LocalDateTime reactivationWindow = LocalDateTime.now().minusMinutes(10);
        Optional<DriverSession> sessionOpt =
                sessionRepository.findRecentlyDisconnectedSession(driverId, reactivationWindow);
        if (sessionOpt.isEmpty()) {
            // No recent disconnected session — new STOMP connection, ignore
            return;
        }
        DriverSession session = sessionOpt.get();
        session.setStatus(DriverSessionStatus.ONLINE);
        session.setLastHeartbeat(LocalDateTime.now());
        sessionRepository.save(session);
        log.info("🟢 Driver {} session {} reactivated after STOMP reconnect", driverId, session.getId());
        broadcastDriverStatus(session, driverId);
    }

    /**
     * Verify that driver owns the session for the given ambulance.
     * Used for authorization checks.
     */
    @Transactional(readOnly = true)
    public boolean isDriverOperatingAmbulance(Long driverId, Long ambulanceId) {
        return sessionRepository.findActiveSessionByDriverAndAmbulance(driverId, ambulanceId)
                .isPresent();
    }

    /**
     * Heartbeat-tolerant version of {@link #isDriverOperatingAmbulance}.
     *
     * Accepts ONLINE/ON_TRIP sessions immediately.
     * For sessions that show OFFLINE (Cloudflare killed STOMP), also accepts
     * sessions where {@code lastHeartbeat >= now - 2 min} and auto-reactivates
     * them to ONLINE so downstream status checks remain consistent.
     *
     * Used by the accept-emergency flow.
     */
    @Transactional
    public boolean isDriverOperatingAmbulanceHeartbeatTolerant(Long driverId, Long ambulanceId) {
        // Fast path: ONLINE or ON_TRIP session exists
        if (isDriverOperatingAmbulance(driverId, ambulanceId)) {
            return true;
        }
        // Slow path: any open (not ended) session with recent heartbeat
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(2);
        return sessionRepository.findAnyActiveSessionByDriverAndAmbulance(driverId, ambulanceId)
                .filter(s -> s.getLastHeartbeat() != null && !s.getLastHeartbeat().isBefore(cutoff))
                .map(s -> {
                    log.warn("Driver {} / ambulance {} session OFFLINE but recent heartbeat — "
                            + "auto-reactivating to ONLINE for accept flow", driverId, ambulanceId);
                    s.setStatus(DriverSessionStatus.ONLINE);
                    sessionRepository.save(s);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Get driver session — heartbeat-tolerant.
     *
     * Returns ONLINE/ON_TRIP sessions immediately.
     * For OFFLINE sessions with a recent REST heartbeat (< 2 min), auto-reactivates
     * them to ONLINE and returns the reactivated session. This covers the Cloudflare
     * STOMP-drop scenario where the driver is alive (REST heartbeat still running)
     * but the WebSocket was killed.
     *
     * Used by the accept-emergency authorization check.
     */
    @Transactional
    public DriverSession getOrReactivateSession(Long driverId) {
        // Fast path
        DriverSession active = getActiveSession(driverId);
        if (active != null) {
            return active;
        }
        // Slow path: any open session with recent heartbeat
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(2);
        return sessionRepository.findAnyActiveSessionByDriverId(driverId)
                .filter(s -> s.getLastHeartbeat() != null && !s.getLastHeartbeat().isBefore(cutoff))
                .map(s -> {
                    log.warn("Driver {} session OFFLINE but recent heartbeat — "
                            + "auto-reactivating to ONLINE for accept flow", driverId);
                    s.setStatus(DriverSessionStatus.ONLINE);
                    return sessionRepository.save(s);
                })
                .orElse(null);
    }

    /**
     * Cleanup stale sessions (admin/cron job).
     * Sessions older than 24 hours are automatically ended.
     */
    @Transactional
    public int cleanupStaleSessions() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(SESSION_CLEANUP_HOURS);
        List<DriverSession> staleSessions = sessionRepository.findStaleSessions(cutoffTime);

        int count = 0;
        for (DriverSession session : staleSessions) {
            try {
                // Force end stale session
                session.setStatus(DriverSessionStatus.OFFLINE);
                session.setSessionEndTime(LocalDateTime.now());
                sessionRepository.save(session);

                log.warn("⚠️ Auto-ended stale session: Driver {}, Ambulance {}, Started at {}",
                        session.getDriverId(),
                        session.getAmbulanceId(),
                        session.getSessionStartTime());

                count++;
            } catch (Exception e) {
                log.error("Failed to cleanup stale session {}: {}", session.getId(), e.getMessage());
            }
        }

        if (count > 0) {
            log.info("Cleaned up {} stale driver sessions", count);
        }

        return count;
    }

    /**
     * Detect and mark drivers with stale GPS heartbeat as OFFLINE.
     * 
     * CRITICAL FOR PRODUCTION:
     * - Driver app sends GPS every 3-5 seconds
     * - If no heartbeat for 30+ seconds → network issue, app crash, or phone dead
     * - Auto-mark driver OFFLINE to prevent assigning emergencies to unavailable
     * drivers
     * - If driver was ON_TRIP, emergency needs manual intervention/reassignment
     * 
     * Called by scheduled job every 15 seconds.
     * 
     * @return Number of drivers marked OFFLINE due to stale heartbeat
     */
    @Transactional
    public int detectAndMarkStaleDriversOffline() {
        List<DriverSession> activeSessions = sessionRepository.findActiveSessions();

        int markedOfflineCount = 0;
        int driversOnTripCount = 0;

        for (DriverSession session : activeSessions) {
            if (session.isStale()) {
                metrics.staleDriverDetected();

                boolean wasOnTrip = session.getStatus() == DriverSessionStatus.ON_TRIP;

                try {
                    // End session (set session_end_time) without changing status
                    // This avoids violating uk_active_driver constraint
                    session.setSessionEndTime(LocalDateTime.now());
                    session.setUpdatedAt(LocalDateTime.now());
                    sessionRepository.save(session);

                    metrics.driverAutoOffline();

                    if (wasOnTrip) {
                        log.error("🚨 CRITICAL: Driver {} session ended during active trip! " +
                                "Session ID: {}, Ambulance: {}, Last heartbeat: {} seconds ago. " +
                                "MANUAL INTERVENTION REQUIRED - Emergency may need reassignment.",
                                session.getDriverId(),
                                session.getId(),
                                session.getAmbulanceId(),
                                Duration.between(
                                        session.getLastHeartbeat() != null ? session.getLastHeartbeat()
                                                : session.getSessionStartTime(),
                                        LocalDateTime.now()).getSeconds());
                        driversOnTripCount++;
                    } else {
                        log.warn("⚠️ Driver {} session ended due to stale heartbeat. " +
                                "Session ID: {}, Ambulance: {}, Last heartbeat: {}",
                                session.getDriverId(),
                                session.getId(),
                                session.getAmbulanceId(),
                                session.getLastHeartbeat() != null ? session.getLastHeartbeat() : "NEVER");
                    }

                    markedOfflineCount++;

                } catch (Exception e) {
                    log.error("Failed to end stale driver {} session: {}",
                            session.getDriverId(), e.getMessage(), e);
                }
            }
        }

        if (markedOfflineCount > 0) {
            log.info("Stale heartbeat detection: Marked {} driver(s) OFFLINE ({} were ON_TRIP)",
                    markedOfflineCount, driversOnTripCount);
        }

        return markedOfflineCount;
    }

    /**
     * Get all online driver sessions (for dispatch service).
     */
    @Transactional(readOnly = true)
    public List<DriverSession> getAllOnlineDrivers() {
        return sessionRepository.findAllOnlineDrivers();
    }

    /**
     * Get driver's session history.
     */
    @Transactional(readOnly = true)
    public List<DriverSession> getDriverHistory(Long driverId) {
        return sessionRepository.findAllByDriverId(driverId);
    }

    /**
     * Get driver's session history with pagination.
     */
    @Transactional(readOnly = true)
    public Page<DriverSession> getDriverHistory(Long driverId, Pageable pageable) {
        return sessionRepository.findHistoryByDriverId(driverId, pageable);
    }

    /**
     * Get driver's active session (non-Optional version).
     * Returns null if no active session exists.
     * Used by authorization and cancellation services.
     */
    @Transactional(readOnly = true)
    public DriverSession getActiveSession(Long driverId) {
        return getCurrentSession(driverId).orElse(null);
    }

    /**
     * Save driver session.
     * Used for status updates during cancellation handling.
     */
    @Transactional
    public DriverSession saveSession(DriverSession session) {
        return sessionRepository.save(session);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Broadcast driver status change to /topic/driver-status so the admin
     * dashboard updates in real-time without polling.
     */
    private void broadcastDriverStatus(DriverSession session, Long driverId) {
        try {
            String driverName = userRepository.findById(driverId)
                    .map(u -> u.getName())
                    .orElse("Driver #" + driverId);

            String licensePlate = ambulanceRepository.findById(session.getAmbulanceId())
                    .map(a -> a.getLicensePlate())
                    .orElse("Amb #" + session.getAmbulanceId());

            DriverStatusDTO dto = new DriverStatusDTO(
                    driverId,
                    driverName,
                    session.getAmbulanceId(),
                    licensePlate,
                    session.getStatus(),
                    session.getCurrentLat(),
                    session.getCurrentLng());

            messagingTemplate.convertAndSend("/topic/driver-status", dto);
            log.debug("Broadcast driver-status: driver={}, status={}", driverId, session.getStatus());
        } catch (Exception e) {
            // Non-critical — don't fail the transaction if broadcast fails
            log.warn("Failed to broadcast driver status for driver {}: {}", driverId, e.getMessage());
        }
    }
}
