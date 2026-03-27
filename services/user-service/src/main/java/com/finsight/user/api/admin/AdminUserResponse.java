package com.finsight.user.api.admin;

import java.time.Instant;

public record AdminUserResponse(
        String email,
        String role,
        boolean active,
        Instant createdAt,
        Instant deactivatedAt) {
}

