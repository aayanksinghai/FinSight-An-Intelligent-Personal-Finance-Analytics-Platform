package com.finsight.notification.security;

import java.security.Principal;

/**
 * A simple Principal wrapping the user's email extracted from their JWT.
 * Spring's user-destination resolver uses Principal.getName() to route
 * messages to /user/{name}/queue/alerts.
 */
public record JwtPrincipal(String email) implements Principal {

    @Override
    public String getName() {
        return email;
    }
}
