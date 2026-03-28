package com.finsight.user.api.auth;

public record PasswordPolicyResponse(String regex, String hint, int minLength) {
}

