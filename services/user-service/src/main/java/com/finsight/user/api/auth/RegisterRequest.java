package com.finsight.user.api.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegisterRequest(
        @Email @NotBlank String email,
        @NotBlank
        @Pattern(
                regexp = PasswordPolicy.REGEX,
                message = PasswordPolicy.MESSAGE)
        String password) {
}

