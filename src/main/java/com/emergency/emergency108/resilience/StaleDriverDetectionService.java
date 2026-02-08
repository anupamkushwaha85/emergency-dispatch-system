package com.emergency.emergency108.resilience;

import com.emergency.emergency108.service.DriverSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Production-critical scheduled service to detect drivers with stale GPS heartbeat.
 * 
 * PROBLEM:
 * - Driver app sends GPS every 3-5 seconds while online
 * - If driver's phone dies, app crashes, or network fails → No more GPS
 * - Without this service, stale drivers stay "ONLINE" and get assigned emergencies they can't respond to
 * 
 * SOLUTION:
 * - Every 15 seconds, check all active driver sessions
 * - If no heartbeat for 30+ seconds → Auto-mark driver OFFLINE
 * - If driver was ON_TRIP → Log CRITICAL alert for manual intervention
 * 
 * This ensures dispatch system only assigns emergencies to truly available drivers.
 */
@Service
public class StaleDriverDetectionService {

    private static final Logger log = LoggerFactory.getLogger(StaleDriverDetectionService.class);

    private final DriverSessionService driverSessionService;

    public StaleDriverDetectionService(DriverSessionService driverSessionService) {
        this.driverSessionService = driverSessionService;
    }

    /**
     * Detect and mark drivers with stale heartbeat as OFFLINE.
     * 
     * Runs every 15 seconds for near real-time detection.
     * 
     * CRITICAL: This prevents assigning emergencies to unavailable drivers.
     */
    @Scheduled(fixedRate = 15000) // 15 seconds
    public void detectStaleDrivers() {
        try {
            int markedOffline = driverSessionService.detectAndMarkStaleDriversOffline();
            
            if (markedOffline > 0) {
                log.info("Stale driver detection completed: {} driver(s) marked OFFLINE", markedOffline);
            }
            
        } catch (Exception e) {
            log.error("CRITICAL ERROR in stale driver detection service: {}", e.getMessage(), e);
            // Don't throw - let scheduler continue on next run
        }
    }
}
