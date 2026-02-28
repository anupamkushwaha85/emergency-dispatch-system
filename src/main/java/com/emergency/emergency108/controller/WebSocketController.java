package com.emergency.emergency108.controller;

import com.emergency.emergency108.config.StompSessionRegistry;
import com.emergency.emergency108.dto.LocationUpdateRequest;
import com.emergency.emergency108.service.DriverSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

/**
 * STOMP message controller — handles inbound frames sent by the Flutter driver app.
 *
 * Client sends to: /app/driver.location
 * Payload: { "lat": 12.345, "lng": 77.456 }
 *
 * The driver's identity is resolved from the STOMP session ID (set during CONNECT)
 * via StompSessionRegistry — the client cannot spoof a different driver ID.
 */
@Controller
public class WebSocketController {

    private static final Logger log = LoggerFactory.getLogger(WebSocketController.class);

    private final StompSessionRegistry registry;
    private final DriverSessionService sessionService;

    public WebSocketController(StompSessionRegistry registry, DriverSessionService sessionService) {
        this.registry = registry;
        this.sessionService = sessionService;
    }

    /**
     * Receive a driver's GPS location over STOMP.
     *
     * Called every time the Flutter app fires a position update (on movement ≥5 m)
     * or the 30-second stationary heartbeat timer triggers.
     *
     * Internally calls {@link DriverSessionService#updateLocation} which:
     *  - Updates the DriverSession's currentLat/Lng and lastHeartbeat
     *  - Saves the Ambulance entity's location
     *  - Broadcasts the new location to /topic/live-locations (admin LiveMap)
     */
    @MessageMapping("/driver.location")
    public void handleDriverLocation(
            @Payload LocationUpdateRequest payload,
            SimpMessageHeaderAccessor headerAccessor) {

        String sessionId = headerAccessor.getSessionId();
        Long driverId = registry.getDriverId(sessionId);

        if (driverId == null) {
            // Frame arrived from a non-driver connection (admin / user) — drop silently
            log.debug("Received driver.location frame from unregistered session {}", sessionId);
            return;
        }

        try {
            sessionService.updateLocation(driverId, payload.getLat(), payload.getLng());
            log.debug("📍 STOMP location update: driver={} ({}, {})", driverId, payload.getLat(), payload.getLng());
        } catch (Exception e) {
            // Non-fatal — driver may have ended their shift between frames
            log.warn("Failed to update location for driver {} via STOMP: {}", driverId, e.getMessage());
        }
    }
}
