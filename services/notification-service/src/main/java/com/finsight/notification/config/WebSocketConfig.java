package com.finsight.notification.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configures the STOMP over WebSocket message broker.
 *
 * The frontend connects to ws://localhost:8090/ws (via the gateway) using SockJS.
 * On a successful STOMP CONNECT (with a valid Bearer token), each user is subscribed
 * to /user/queue/alerts. The server pushes alerts via convertAndSendToUser().
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                // Allow all origins in dev; tighten in prod with specific gateway origin
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Simple in-memory broker for /queue (point-to-point) and /topic (broadcast)
        registry.enableSimpleBroker("/queue", "/topic");
        // Prefix for messages from clients to @MessageMapping handlers (none used here)
        registry.setApplicationDestinationPrefixes("/app");
        // Prefix that maps user-specific destinations: /user/{email}/queue/alerts
        registry.setUserDestinationPrefix("/user");
    }
}
