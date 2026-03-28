package com.finsight.user.api.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PasswordResetConfirmRequest(
        @NotBlank String resetToken,
        @NotBlank
        @Pattern(
                regexp = PasswordPolicy.REGEX,
                message = PasswordPolicy.MESSAGE)
        String newPassword) {
}

