package com.finsight.user.security;

import com.finsight.user.api.auth.PasswordResetResponse;
import com.finsight.user.persistence.PasswordResetToken;
import com.finsight.user.persistence.PasswordResetTokenRepository;
import com.finsight.user.persistence.UserCredential;
import com.finsight.user.persistence.UserCredentialRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PasswordResetService {

    private final UserCredentialRepository userCredentialRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final DevAuthService devAuthService;
    private final long resetExpirySeconds;

    public PasswordResetService(
            UserCredentialRepository userCredentialRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            DevAuthService devAuthService,
            @Value("${security.auth.password-reset-expiry-seconds:900}") long resetExpirySeconds) {
        this.userCredentialRepository = userCredentialRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.devAuthService = devAuthService;
        this.resetExpirySeconds = resetExpirySeconds;
    }

    @Transactional
    public PasswordResetResponse requestPasswordReset(String email) {
        String normalizedEmail = normalizeEmail(email);
        UserCredential user = userCredentialRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String resetToken = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        PasswordResetToken token = new PasswordResetToken();
        token.setToken(resetToken);
        token.setUserEmail(user.getEmail());
        token.setExpiresAt(Instant.now().plusSeconds(resetExpirySeconds));
        passwordResetTokenRepository.save(token);

        return new PasswordResetResponse("Password reset token generated", resetToken, resetExpirySeconds);
    }

    @Transactional
    public void confirmPasswordReset(String resetToken, String newPassword) {
        PasswordResetToken token = passwordResetTokenRepository.findByToken(resetToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid reset token"));

        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Reset token is expired or already used");
        }

        token.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(token);
        devAuthService.resetPasswordWithoutCurrent(token.getUserEmail(), newPassword);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}

