package com.finsight.user.security;

import com.finsight.user.persistence.PasswordResetTokenRepository;
import com.finsight.user.persistence.RefreshTokenSessionRepository;
import com.finsight.user.persistence.RevokedAccessTokenRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenCleanupService {

    private static final Logger log = LoggerFactory.getLogger(TokenCleanupService.class);

    private final RefreshTokenSessionRepository refreshTokenSessionRepository;
    private final RevokedAccessTokenRepository revokedAccessTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final long cleanupRetentionSeconds;

    public TokenCleanupService(
            RefreshTokenSessionRepository refreshTokenSessionRepository,
            RevokedAccessTokenRepository revokedAccessTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            @Value("${security.tokens.cleanup-retention-seconds:259200}") long cleanupRetentionSeconds) {
        this.refreshTokenSessionRepository = refreshTokenSessionRepository;
        this.revokedAccessTokenRepository = revokedAccessTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.cleanupRetentionSeconds = cleanupRetentionSeconds;
    }

    @Scheduled(fixedDelayString = "${security.tokens.cleanup-interval-ms:900000}")
    @Transactional
    public void cleanupExpiredTokens() {
        Instant now = Instant.now();
        Instant cleanupBefore = now.minusSeconds(cleanupRetentionSeconds);

        int removedRefresh = refreshTokenSessionRepository.deleteExpiredOrOldSessions(now, cleanupBefore);
        int removedRevokedAccess = revokedAccessTokenRepository.deleteByExpiresAtBefore(now);
        int removedResetTokens = passwordResetTokenRepository.deleteByExpiresAtBefore(now);

        if (removedRefresh > 0 || removedRevokedAccess > 0 || removedResetTokens > 0) {
            log.info(
                    "Token cleanup removed {} refresh sessions, {} revoked access tokens, {} password reset tokens",
                    removedRefresh,
                    removedRevokedAccess,
                    removedResetTokens);
        }
    }
}
