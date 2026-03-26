package com.finsight.user.security;

import com.finsight.user.persistence.RefreshTokenSessionRepository;
import com.finsight.user.persistence.RevokedAccessTokenRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class TokenCleanupServiceTest {

    @Test
    void cleanupShouldInvokeRepositoryDeletionMethods() {
        RefreshTokenSessionRepository refreshRepo = Mockito.mock(RefreshTokenSessionRepository.class);
        RevokedAccessTokenRepository revokedRepo = Mockito.mock(RevokedAccessTokenRepository.class);

        TokenCleanupService cleanupService = new TokenCleanupService(refreshRepo, revokedRepo, 3600);
        cleanupService.cleanupExpiredTokens();

        Mockito.verify(refreshRepo).deleteExpiredOrOldSessions(Mockito.any(), Mockito.any());
        Mockito.verify(revokedRepo).deleteByExpiresAtBefore(Mockito.any());
    }
}

