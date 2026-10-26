package com.example.event_ticketing.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Live seat map transport. Clients open a STOMP-over-WebSocket connection at /ws and subscribe to
 * /topic/events/{eventId}/seats; the server pushes a small "seats changed" nudge there whenever a
 * seat is held, released or booked, so every open seat map updates instantly instead of waiting for
 * its poll. An in-memory simple broker is enough: the app runs as a single instance and the messages
 * are ephemeral notifications, not durable data.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Same-origin in production (the SPA is served by this app); allowed broadly so the Vite dev
        // server can connect too. Raw WebSocket (no SockJS) keeps the client dependency minimal.
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
    }
}
