package com.hackathon.emergency108.service;

import com.hackathon.emergency108.entity.*;
import com.hackathon.emergency108.repository.AmbulanceRepository;
import com.hackathon.emergency108.repository.DriverSessionRepository;
import com.hackathon.emergency108.repository.EmergencyAssignmentRepository;
import com.hackathon.emergency108.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Validates driver session invariants on application startup.
 * Logs inconsistencies but does NOT auto-fix them.
 * 
 * Purpose: Detect data corruption or crash recovery issues.
 */
@Service
public class DriverSessionInvariantValidator {

    private static final Logger log = LoggerFactory.getLogger(DriverSessionInvariantValidator.class);

    private final DriverSessionRepository sessionRepository;
    private final EmergencyAssignmentRepository assignmentRepository;
    private final AmbulanceRepository ambulanceRepository;
    private final UserRepository userRepository;

    public DriverSessionInvariantValidator(
            DriverSessionRepository sessionRepository,
            EmergencyAssignmentRepository assignmentRepository,
            AmbulanceRepository ambulanceRepository,
            UserRepository userRepository
    ) {
        this.sessionRepository = sessionRepository;
        this.assignmentRepository = assignmentRepository;
        this.ambulanceRepository = ambulanceRepository;
        this.userRepository = userRepository;
    }

    /**
     * Run invariant validation after application startup.
     * This ensures database is in consistent state before accepting requests.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void validateInvariants() {
        log.info("========================================");
        log.info("🔍 Running Driver Session Invariant Validation...");
        log.info("========================================");

        int totalIssues = 0;

        totalIssues += validateActiveSessionsHaveValidDrivers();
        totalIssues += validateActiveSessionsHaveValidAmbulances();
        totalIssues += validateOnTripDriversHaveActiveAssignments();
        totalIssues += validateMultipleActiveSessionsPerDriver();
        totalIssues += validateMultipleActiveSessionsPerAmbulance();
        totalIssues += validateSessionsWithBlockedDrivers();
        totalIssues += validateSessionsWithUnverifiedDrivers();
        totalIssues += validateStaleActiveSessions();
        totalIssues += validateStaleHeartbeats();

        log.info("========================================");
        if (totalIssues == 0) {
            log.info("✅ Driver Session Invariants: ALL VALID (0 issues found)");
        } else {
            log.warn("⚠️  Driver Session Invariants: {} ISSUES DETECTED", totalIssues);
            log.warn("⚠️  Review logs above and investigate data inconsistencies");
            log.warn("⚠️  Consider running cleanup operations or manual fixes");
        }
        log.info("========================================");
    }

    /**
     * INVARIANT 1: All active sessions must reference existing drivers
     */
    private int validateActiveSessionsHaveValidDrivers() {
        log.debug("Checking: Active sessions have valid drivers...");
        List<DriverSession> activeSessions = sessionRepository.findAll().stream()
                .filter(DriverSession::isActive)
                .toList();

        int issues = 0;
        for (DriverSession session : activeSessions) {
            Optional<User> driver = userRepository.findById(session.getDriverId());
            if (driver.isEmpty()) {
                log.error("❌ INVARIANT VIOLATION: Session {} references non-existent driver {}",
                    session.getId(), session.getDriverId());
                issues++;
            }
        }

        if (issues == 0) {
            log.debug("✅ All active sessions have valid drivers ({} checked)", activeSessions.size());
        }
        return issues;
    }

    /**
     * INVARIANT 2: All active sessions must reference existing ambulances
     */
    private int validateActiveSessionsHaveValidAmbulances() {
        log.debug("Checking: Active sessions have valid ambulances...");
        List<DriverSession> activeSessions = sessionRepository.findAll().stream()
                .filter(DriverSession::isActive)
                .toList();

        int issues = 0;
        for (DriverSession session : activeSessions) {
            Optional<Ambulance> ambulance = ambulanceRepository.findById(session.getAmbulanceId());
            if (ambulance.isEmpty()) {
                log.error("❌ INVARIANT VIOLATION: Session {} references non-existent ambulance {}",
                    session.getId(), session.getAmbulanceId());
                issues++;
            }
        }

        if (issues == 0) {
            log.debug("✅ All active sessions have valid ambulances ({} checked)", activeSessions.size());
        }
        return issues;
    }

    /**
     * INVARIANT 3: Drivers with ON_TRIP status must have an active ACCEPTED assignment
     */
    private int validateOnTripDriversHaveActiveAssignments() {
        log.debug("Checking: ON_TRIP drivers have active assignments...");
        List<DriverSession> onTripSessions = sessionRepository.findAll().stream()
                .filter(s -> s.getStatus() == DriverSessionStatus.ON_TRIP)
                .toList();

        int issues = 0;
        for (DriverSession session : onTripSessions) {
            // Check if driver has an ACCEPTED assignment
            List<EmergencyAssignment> activeAssignments = assignmentRepository.findAll().stream()
                    .filter(a -> session.getDriverId().equals(a.getDriverId()))
                    .filter(a -> a.getStatus() == EmergencyAssignmentStatus.ACCEPTED)
                    .toList();

            if (activeAssignments.isEmpty()) {
                log.error("❌ INVARIANT VIOLATION: Driver {} is ON_TRIP (Session {}) but has no ACCEPTED assignment",
                    session.getDriverId(), session.getId());
                log.error("   Session details: Ambulance {}, Started {}, Emergencies handled {}",
                    session.getAmbulanceId(), session.getSessionStartTime(), session.getEmergenciesHandled());
                issues++;
            } else if (activeAssignments.size() > 1) {
                log.error("❌ INVARIANT VIOLATION: Driver {} has {} ACCEPTED assignments (expected: 1)",
                    session.getDriverId(), activeAssignments.size());
                issues++;
            }
        }

        if (issues == 0 && !onTripSessions.isEmpty()) {
            log.debug("✅ All ON_TRIP drivers have valid assignments ({} checked)", onTripSessions.size());
        }
        return issues;
    }

    /**
     * INVARIANT 4: Each driver can have at most ONE active session
     */
    private int validateMultipleActiveSessionsPerDriver() {
        log.debug("Checking: No driver has multiple active sessions...");
        List<DriverSession> allActiveSessions = sessionRepository.findAll().stream()
                .filter(DriverSession::isActive)
                .toList();

        int issues = 0;
        var driverSessionCount = allActiveSessions.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    DriverSession::getDriverId,
                    java.util.stream.Collectors.counting()
                ));

        for (var entry : driverSessionCount.entrySet()) {
            if (entry.getValue() > 1) {
                log.error("❌ INVARIANT VIOLATION: Driver {} has {} active sessions (expected: 1)",
                    entry.getKey(), entry.getValue());
                
                // Log details of conflicting sessions
                allActiveSessions.stream()
                        .filter(s -> s.getDriverId().equals(entry.getKey()))
                        .forEach(s -> log.error("   - Session {}: Ambulance {}, Status {}, Started {}",
                            s.getId(), s.getAmbulanceId(), s.getStatus(), s.getSessionStartTime()));
                
                issues++;
            }
        }

        if (issues == 0) {
            log.debug("✅ No driver has multiple active sessions ({} unique drivers)", driverSessionCount.size());
        }
        return issues;
    }

    /**
     * INVARIANT 5: Each ambulance can have at most ONE active session
     */
    private int validateMultipleActiveSessionsPerAmbulance() {
        log.debug("Checking: No ambulance has multiple active sessions...");
        List<DriverSession> allActiveSessions = sessionRepository.findAll().stream()
                .filter(DriverSession::isActive)
                .toList();

        int issues = 0;
        var ambulanceSessionCount = allActiveSessions.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    DriverSession::getAmbulanceId,
                    java.util.stream.Collectors.counting()
                ));

        for (var entry : ambulanceSessionCount.entrySet()) {
            if (entry.getValue() > 1) {
                log.error("❌ INVARIANT VIOLATION: Ambulance {} has {} active sessions (expected: 1)",
                    entry.getKey(), entry.getValue());
                
                // Log details of conflicting sessions
                allActiveSessions.stream()
                        .filter(s -> s.getAmbulanceId().equals(entry.getKey()))
                        .forEach(s -> log.error("   - Session {}: Driver {}, Status {}, Started {}",
                            s.getId(), s.getDriverId(), s.getStatus(), s.getSessionStartTime()));
                
                issues++;
            }
        }

        if (issues == 0) {
            log.debug("✅ No ambulance has multiple active sessions ({} unique ambulances)", ambulanceSessionCount.size());
        }
        return issues;
    }

    /**
     * INVARIANT 6: Active sessions should not have blocked drivers
     */
    private int validateSessionsWithBlockedDrivers() {
        log.debug("Checking: No active session with blocked drivers...");
        List<DriverSession> activeSessions = sessionRepository.findAll().stream()
                .filter(DriverSession::isActive)
                .toList();

        int issues = 0;
        for (DriverSession session : activeSessions) {
            Optional<User> driverOpt = userRepository.findById(session.getDriverId());
            if (driverOpt.isPresent() && driverOpt.get().isBlocked()) {
                log.error("❌ INVARIANT VIOLATION: Active session {} has blocked driver {}",
                    session.getId(), session.getDriverId());
                log.error("   Session: Ambulance {}, Status {}, Started {}",
                    session.getAmbulanceId(), session.getStatus(), session.getSessionStartTime());
                issues++;
            }
        }

        if (issues == 0) {
            log.debug("✅ No active sessions with blocked drivers");
        }
        return issues;
    }

    /**
     * INVARIANT 7: Active sessions should only have VERIFIED drivers
     */
    private int validateSessionsWithUnverifiedDrivers() {
        log.debug("Checking: All active sessions have verified drivers...");
        List<DriverSession> activeSessions = sessionRepository.findAll().stream()
                .filter(DriverSession::isActive)
                .toList();

        int issues = 0;
        for (DriverSession session : activeSessions) {
            Optional<User> driverOpt = userRepository.findById(session.getDriverId());
            if (driverOpt.isPresent()) {
                User driver = driverOpt.get();
                if (driver.getDriverVerificationStatus() != DriverVerificationStatus.VERIFIED) {
                    log.error("❌ INVARIANT VIOLATION: Active session {} has unverified driver {} (status: {})",
                        session.getId(), session.getDriverId(), driver.getDriverVerificationStatus());
                    issues++;
                }
            }
        }

        if (issues == 0) {
            log.debug("✅ All active sessions have verified drivers");
        }
        return issues;
    }

    /**
     * SANITY CHECK: Warn about very old active sessions (possible crash recovery issue)
     */
    private int validateStaleActiveSessions() {
        log.debug("Checking: No stale active sessions (>24 hours)...");
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        List<DriverSession> staleSessions = sessionRepository.findAll().stream()
                .filter(DriverSession::isActive)
                .filter(s -> s.getSessionStartTime().isBefore(cutoff))
                .toList();

        int issues = 0;
        for (DriverSession session : staleSessions) {
            Duration age = Duration.between(session.getSessionStartTime(), LocalDateTime.now());
            log.warn("⚠️  STALE SESSION: Session {} has been active for {} hours (Driver {}, Ambulance {}, Status {})",
                session.getId(), age.toHours(), session.getDriverId(), session.getAmbulanceId(), session.getStatus());
            log.warn("   Started: {}, Emergencies handled: {}",
                session.getSessionStartTime(), session.getEmergenciesHandled());
            issues++;
        }

        if (issues == 0) {
            log.debug("✅ No stale active sessions found");
        } else {
            log.warn("⚠️  {} stale sessions detected. Consider running cleanup job.", issues);
        }
        return issues;
    }

    /**
     * INVARIANT 8: Active ONLINE/ON_TRIP sessions should have fresh GPS heartbeat
     * 
     * CRITICAL: Stale heartbeats indicate crashed apps, dead phones, or network issues.
     * These drivers should be auto-marked OFFLINE by StaleDriverDetectionService.
     */
    private int validateStaleHeartbeats() {
        log.debug("Checking: Active sessions have fresh GPS heartbeat (<30 seconds)...");
        List<DriverSession> activeSessions = sessionRepository.findAll().stream()
                .filter(DriverSession::isActive)
                .filter(s -> s.getStatus() == DriverSessionStatus.ONLINE || s.getStatus() == DriverSessionStatus.ON_TRIP)
                .toList();

        int issues = 0;
        int noHeartbeatCount = 0;
        int staleHeartbeatCount = 0;

        for (DriverSession session : activeSessions) {
            if (session.getLastHeartbeat() == null) {
                log.warn("⚠️  MISSING HEARTBEAT: Session {} (Driver {}, Ambulance {}) has NEVER sent GPS heartbeat",
                    session.getId(), session.getDriverId(), session.getAmbulanceId());
                log.warn("   Status: {}, Started: {}, Location: ({}, {})",
                    session.getStatus(), session.getSessionStartTime(),
                    session.getCurrentLat(), session.getCurrentLng());
                noHeartbeatCount++;
                issues++;
            } else if (session.isStale()) {
                long secondsSinceHeartbeat = Duration.between(session.getLastHeartbeat(), LocalDateTime.now()).getSeconds();
                log.warn("⚠️  STALE HEARTBEAT: Session {} (Driver {}, Ambulance {}) last GPS was {} seconds ago",
                    session.getId(), session.getDriverId(), session.getAmbulanceId(), secondsSinceHeartbeat);
                log.warn("   Status: {}, Last heartbeat: {}, Should be auto-marked OFFLINE",
                    session.getStatus(), session.getLastHeartbeat());
                
                if (session.getStatus() == DriverSessionStatus.ON_TRIP) {
                    log.error("🚨 CRITICAL: Driver is ON_TRIP with stale heartbeat! Emergency may be stranded.");
                }
                
                staleHeartbeatCount++;
                issues++;
            }
        }

        if (issues == 0) {
            log.debug("✅ All active sessions have fresh heartbeat ({} checked)", activeSessions.size());
        } else {
            log.warn("⚠️  Heartbeat issues: {} sessions with no heartbeat, {} with stale heartbeat",
                noHeartbeatCount, staleHeartbeatCount);
            log.warn("⚠️  StaleDriverDetectionService should auto-fix these within 15 seconds");
        }
        
        return issues;
    }
}
