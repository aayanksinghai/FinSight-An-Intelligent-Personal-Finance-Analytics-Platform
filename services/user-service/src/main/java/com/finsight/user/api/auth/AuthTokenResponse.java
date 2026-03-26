package com.finsight.user.api.auth;

public record AuthTokenResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds) {
}

