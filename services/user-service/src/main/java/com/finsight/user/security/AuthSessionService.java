package com.finsight.user.security;

import com.finsight.user.api.auth.AuthTokenResponse;
import com.finsight.user.persistence.RefreshTokenSession;
import com.finsight.user.persistence.RefreshTokenSessionRepository;
import com.finsight.user.persistence.RevokedAccessToken;
import com.finsight.user.persistence.RevokedAccessTokenRepository;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthSessionService {

    private final JwtTokenService jwtTokenService;
    private final RefreshTokenSessionRepository refreshTokenSessionRepository;
    private final RevokedAccessTokenRepository revokedAccessTokenRepository;

    public AuthSessionService(
            JwtTokenService jwtTokenService,
            RefreshTokenSessionRepository refreshTokenSessionRepository,
            RevokedAccessTokenRepository revokedAccessTokenRepository) {
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenSessionRepository = refreshTokenSessionRepository;
        this.revokedAccessTokenRepository = revokedAccessTokenRepository;
    }

    @Transactional
    public AuthTokenResponse issueSession(String email) {
        JwtTokenService.TokenPair tokenPair = jwtTokenService.issueTokenPair(email);
        persistRefreshSession(email, tokenPair.refreshJti(), tokenPair.refreshExpiresAt());
        return toAuthResponse(tokenPair);
    }

    @Transactional
    public AuthTokenResponse refreshSession(String refreshToken) {
        JwtTokenService.ParsedRefreshToken parsed = jwtTokenService.parseRefreshToken(refreshToken);
        RefreshTokenSession session = refreshTokenSessionRepository.findByTokenJti(parsed.tokenJti())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown refresh token"));

        if (session.getRevokedAt() != null || session.getUsedAt() != null || session.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is no longer valid");
        }

        session.setUsedAt(Instant.now());
        refreshTokenSessionRepository.save(session);

        JwtTokenService.TokenPair tokenPair = jwtTokenService.issueTokenPair(parsed.subjectEmail());
        persistRefreshSession(parsed.subjectEmail(), tokenPair.refreshJti(), tokenPair.refreshExpiresAt());
        return toAuthResponse(tokenPair);
    }

    @Transactional
    public void logout(Jwt accessJwt) {
        revokeAccessToken(accessJwt);
    }

    @Transactional
    public void logoutAllSessions(Jwt accessJwt) {
        revokeAccessToken(accessJwt);
        refreshTokenSessionRepository.revokeActiveByUserEmail(accessJwt.getSubject(), Instant.now());
    }

    private void revokeAccessToken(Jwt accessJwt) {
        Object jtiClaim = accessJwt.getClaims().get("jti");
        if (jtiClaim instanceof String tokenJti && !tokenJti.isBlank()) {
            RevokedAccessToken revoked = new RevokedAccessToken();
            revoked.setTokenJti(tokenJti);
            revoked.setExpiresAt(accessJwt.getExpiresAt());
            revokedAccessTokenRepository.save(revoked);
        }
    }

    private void persistRefreshSession(String email, String refreshJti, Instant refreshExpiresAt) {
        RefreshTokenSession session = new RefreshTokenSession();
        session.setUserEmail(email);
        session.setTokenJti(refreshJti);
        session.setExpiresAt(refreshExpiresAt);
        refreshTokenSessionRepository.save(session);
    }

    private AuthTokenResponse toAuthResponse(JwtTokenService.TokenPair tokenPair) {
        return new AuthTokenResponse(
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                "Bearer",
                tokenPair.accessExpiresInSeconds(),
                tokenPair.refreshExpiresInSeconds());
    }
}

