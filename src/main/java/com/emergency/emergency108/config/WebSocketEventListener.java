package com.emergency.emergency108.config;

import com.emergency.emergency108.service.DriverSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.List;

/**
 * Listens to STOMP lifecycle events to maintain driver online/offline state.
 *
 * CONNECT:    Reads the 'driverId' header the Flutter app sends in every STOMP
 *             CONNECT frame and registers the mapping in StompSessionRegistry.
 *
 * DISCONNECT: Looks up the driverId from the registry and marks the driver's
 *             session OFFLINE immediately — no heartbeat timeout required.
 *             This fires for all disconnect causes: clean app close, network drop,
 *             phone battery death, or OS kill.
 */
@Component
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final StompSessionRegistry registry;
    private final DriverSessionService sessionService;

    public WebSocketEventListener(StompSessionRegistry registry, DriverSessionService sessionService) {
        this.registry = registry;
        this.sessionService = sessionService;
    }

    @EventListener
    public void handleConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        List<String> driverIdHeaders = accessor.getNativeHeader("driverId");
        if (driverIdHeaders == null || driverIdHeaders.isEmpty()) {
            // Admin / user connection — no driverId header expected
            return;
        }

        try {
            Long driverId = Long.parseLong(driverIdHeaders.get(0).trim());
            registry.register(sessionId, driverId);
            log.info("🟢 Driver {} connected via STOMP (session={})", driverId, sessionId);

            // If the driver had a session that was set OFFLINE by a previous STOMP disconnect
            // (network drop, app background, etc.) reactivate it so dispatch can find them.
            sessionService.reactivateIfDisconnected(driverId);
        } catch (NumberFormatException e) {
            log.warn("Invalid driverId in STOMP CONNECT headers: '{}'", driverIdHeaders.get(0));
        }
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        Long driverId = registry.getDriverId(sessionId);
        if (driverId == null) {
            // Not a driver connection (admin / user) — nothing to do
            return;
        }

        registry.remove(sessionId);

        try {
            sessionService.markDriverOfflineFromDisconnect(driverId);
            log.info("🔴 Driver {} marked OFFLINE — STOMP socket disconnected (session={})", driverId, sessionId);
        } catch (Exception e) {
            // Non-fatal: driver may have already ended their shift cleanly
            log.warn("Could not mark driver {} OFFLINE on disconnect: {}", driverId, e.getMessage());
        }
    }
}
