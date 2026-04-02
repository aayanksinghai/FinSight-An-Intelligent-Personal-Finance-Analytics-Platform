package com.finsight.notification.config;

import com.finsight.notification.security.JwtPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Validates the JWT Bearer token carried in the STOMP CONNECT frame.
 *
 * The frontend sends: connectHeaders: { Authorization: "Bearer <token>" }
 * This interceptor extracts that header, validates the RSA-256 signature,
 * and installs a JwtPrincipal so Spring can route /user/queue/alerts correctly.
 *
 * Any CONNECT frame without a valid JWT is rejected with an IllegalArgumentException
 * which causes the WebSocket connection to be refused before any subscription.
 */
@Configuration
public class WebSocketSecurityConfig implements WebSocketMessageBrokerConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebSocketSecurityConfig.class);

    @Autowired
    private JwtChannelAuthenticator jwtChannelAuthenticator;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor == null) {
                    return message;
                }

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                        log.warn("STOMP CONNECT rejected: missing or malformed Authorization header");
                        throw new IllegalArgumentException("Missing Authorization header");
                    }
                    String token = authHeader.substring(7);
                    JwtPrincipal principal = jwtChannelAuthenticator.authenticate(token);
                    accessor.setUser(principal);
                    log.debug("STOMP CONNECT authenticated for user: {}", principal.getName());
                }
                return message;
            }
        });
    }
}
