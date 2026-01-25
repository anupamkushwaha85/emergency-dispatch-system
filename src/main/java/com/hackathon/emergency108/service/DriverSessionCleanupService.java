package com.hackathon.emergency108.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Scheduled job to cleanup stale driver sessions.
 * Runs every hour to end sessions older than 24 hours.
 */
@Service
public class DriverSessionCleanupService {

    private static final Logger log = LoggerFactory.getLogger(DriverSessionCleanupService.class);

    private final DriverSessionService driverSessionService;

    public DriverSessionCleanupService(DriverSessionService driverSessionService) {
        this.driverSessionService = driverSessionService;
    }

    /**
     * Cleanup stale sessions every hour.
     * Sessions older than 24 hours are automatically ended.
     * Delays first run by 60s to avoid conflict with DriverRevivalRunner.
     */
    @Scheduled(fixedRateString = "3600000", initialDelayString = "60000")
    public void cleanupStaleSessions() {
        try {
            log.debug("Running driver session cleanup job...");
            int cleaned = driverSessionService.cleanupStaleSessions();

            if (cleaned > 0) {
                log.info("✅ Cleaned up {} stale driver sessions", cleaned);
            } else {
                log.debug("No stale sessions found");
            }

        } catch (Exception e) {
            log.error("❌ Driver session cleanup failed: {}", e.getMessage(), e);
        }
    }
}
