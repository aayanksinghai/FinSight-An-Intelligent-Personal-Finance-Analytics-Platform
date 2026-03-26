package com.finsight.apigateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class JwtStubFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtStubFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // Placeholder for RS256 JWT verification to be added in the auth slice.
            log.debug("JWT stub filter received bearer token for path: {}", exchange.getRequest().getPath());
        }
        return chain.filter(exchange);
    }
}

