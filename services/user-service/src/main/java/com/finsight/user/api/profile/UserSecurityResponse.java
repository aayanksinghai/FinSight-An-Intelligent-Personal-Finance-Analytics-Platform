package com.finsight.user.api.profile;

import java.time.Instant;

public record UserSecurityResponse(
        String email,
        Instant accountCreatedAt,
        Instant lastProfileUpdateAt,
        boolean profileConfigured,
        boolean hasPassword) {
}

