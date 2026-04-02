package com.finsight.notification.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

/**
 * HTTP security configuration.
 *
 * WebSocket connections are validated at the STOMP layer (see WebSocketSecurityConfig).
 * We must permit /ws/** at the HTTP level so the SockJS handshake can complete; the
 * JWT check happens in the STOMP ChannelInterceptor after the connection is established.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(STATELESS))
            .authorizeHttpRequests(auth -> auth
                // SockJS polling fallback and WebSocket upgrade — validated at STOMP layer
                .requestMatchers("/ws/**").permitAll()
                // Kubernetes liveness/readiness + Prometheus scrape
                .requestMatchers(
                    "/actuator/health",
                    "/actuator/health/**",
                    "/actuator/info",
                    "/actuator/prometheus"
                ).permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
