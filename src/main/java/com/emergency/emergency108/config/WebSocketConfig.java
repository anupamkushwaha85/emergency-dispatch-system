package com.emergency.emergency108.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket STOMP configuration.
 *
 * Enables a persistent, bidirectional connection between clients and the
 * server.
 * Drivers (Flutter app) send location updates via STOMP frames.
 * Admin panel (React) subscribes to /topic/live-locations to receive real-time
 * location broadcasts without any HTTP polling.
 *
 * Endpoint: ws://host/ws (with SockJS fallback for browsers)
 * Topics:
 * /topic/live-locations — All driver locations (admin panel subscribes here)
 * /topic/driver/{driverId} — Per-driver channel (for targeted commands)
 */
/**
 * Configuration class for WebSocket settings.
 *
 * @author anupam kushwaha
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configure message broker operation.
     * @param registry the registry
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable a simple in-memory message broker for broadcasting
        registry.enableSimpleBroker("/topic");
        // All messages sent FROM a client must be prefixed with /app
        registry.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Register stomp endpoints operation.
     * @param registry the registry
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Primary WebSocket endpoint. Flutter app uses raw ws://host/ws
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*"); // CORS — same pattern as SecurityConfig

        // SockJS fallback for browsers (Admin panel uses this)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
