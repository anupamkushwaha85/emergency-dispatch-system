package com.emergency.emergency108.config;

import com.emergency.emergency108.entity.DriverSession;
import com.emergency.emergency108.entity.DriverSessionStatus;
import com.emergency.emergency108.repository.DriverSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * TEMPORARY FIXER: Revives all driver sessions on startup.
 * Sets them to ONLINE and updates heartbeat so they are valid for dispatch.
 */
@Component
public class DriverRevivalRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DriverRevivalRunner.class);
    private final DriverSessionRepository sessionRepository;

    public DriverRevivalRunner(DriverSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("🚑 STARTING DRIVER REVIVAL PROTOCOL...");

        List<DriverSession> allSessions = sessionRepository.findAll();
        int count = 0;

        for (DriverSession session : allSessions) {
            // Revive everyone for testing
            session.setStatus(DriverSessionStatus.ONLINE);
            session.setLastHeartbeat(LocalDateTime.now());
            session.setSessionStartTime(LocalDateTime.now()); // Reset start time to be fresh
            session.setSessionEndTime(null); // Clear end time if any

            sessionRepository.save(session);
            count++;
        }

        log.info("✅ REVIVIED {} DRIVER SESSIONS. All drivers are now ONLINE and FRESH.", count);
    }
}
