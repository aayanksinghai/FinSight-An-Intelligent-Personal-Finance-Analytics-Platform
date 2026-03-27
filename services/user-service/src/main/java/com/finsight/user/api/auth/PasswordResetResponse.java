package com.finsight.user.api.auth;

public record PasswordResetResponse(String message, String resetToken, long expiresInSeconds) {
}

