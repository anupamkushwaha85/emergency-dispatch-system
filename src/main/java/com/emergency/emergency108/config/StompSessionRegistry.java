package com.emergency.emergency108.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry that maps STOMP WebSocket session IDs to driver user IDs.
 *
 * Populated on STOMP CONNECT (from 'driverId' native header sent by the Flutter app).
 * Consumed on STOMP DISCONNECT to identify which driver went offline so we can
 * auto-mark their session OFFLINE without waiting for a heartbeat timeout.
 *
 * Thread-safe: backed by ConcurrentHashMap so concurrent connect/disconnect events
 * from the broker thread pool never race.
 */
/**
 * @author anupam kushwaha
 */
@Component
public class StompSessionRegistry {

    private static final Logger log = LoggerFactory.getLogger(StompSessionRegistry.class);

    /** STOMP session ID → driver user ID */
    private final ConcurrentHashMap<String, Long> sessionToDriver = new ConcurrentHashMap<>();

    public void register(String sessionId, Long driverId) {
        sessionToDriver.put(sessionId, driverId);
        log.debug("Registered STOMP session {} → driver {}", sessionId, driverId);
    }

    /**
     * Returns the driver ID for the given session, or null if not a tracked driver session.
     * (Admin / user connections will return null — that is expected and safe.)
     */
    public Long getDriverId(String sessionId) {
        return sessionToDriver.get(sessionId);
    }

    public void remove(String sessionId) {
        Long removed = sessionToDriver.remove(sessionId);
        if (removed != null) {
            log.debug("Removed STOMP session {} (driver {})", sessionId, removed);
        }
    }

    public int size() {
        return sessionToDriver.size();
    }
}
