package com.finsight.user.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class JwtTokenService {

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final String issuer;
    private final long expirySeconds;
    private final long refreshExpirySeconds;

    public JwtTokenService(
            @Value("${security.jwt.private-key}") String privateKeyPem,
            @Value("${security.jwt.public-key}") String publicKeyPem,
            @Value("${security.jwt.issuer}") String issuer,
            @Value("${security.jwt.expiry-seconds}") long expirySeconds,
            @Value("${security.jwt.refresh-expiry-seconds:604800}") long refreshExpirySeconds) throws Exception {
        this.privateKey = parsePrivateKey(privateKeyPem);
        this.publicKey = parsePublicKey(publicKeyPem);
        this.issuer = issuer;
        this.expirySeconds = expirySeconds;
        this.refreshExpirySeconds = refreshExpirySeconds;
    }

    public TokenPair issueTokenPair(String subjectEmail, String role) {
        Instant now = Instant.now();
        String accessJti = UUID.randomUUID().toString();
        String refreshJti = UUID.randomUUID().toString();
        String normalizedRole = role == null ? "USER" : role;

        String accessToken = Jwts.builder()
                .subject(subjectEmail)
                .issuer(issuer)
                .id(accessJti)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirySeconds)))
                .claim("typ", "access")
                .claim("role", normalizedRole)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();

        Instant refreshExpiry = now.plusSeconds(refreshExpirySeconds);
        String refreshToken = Jwts.builder()
                .subject(subjectEmail)
                .issuer(issuer)
                .id(refreshJti)
                .issuedAt(Date.from(now))
                .expiration(Date.from(refreshExpiry))
                .claim("typ", "refresh")
                .claim("role", normalizedRole)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();

        return new TokenPair(accessToken, refreshToken, expirySeconds, refreshExpirySeconds, refreshJti, refreshExpiry);
    }

    public ParsedRefreshToken parseRefreshToken(String refreshToken) {
        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(refreshToken)
                    .getPayload();
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        Object type = claims.get("typ");
        if (!"refresh".equals(type)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token type");
        }

        return new ParsedRefreshToken(
                claims.getSubject(),
                claims.getId(),
                claims.get("role", String.class),
                claims.getExpiration().toInstant());
    }

    public long getExpirySeconds() {
        return expirySeconds;
    }

    public long getRefreshExpirySeconds() {
        return refreshExpirySeconds;
    }

    private PrivateKey parsePrivateKey(String privateKeyPem) throws Exception {
        String normalized = privateKeyPem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] decoded = Base64.getDecoder().decode(normalized);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
        return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
    }

    private PublicKey parsePublicKey(String publicKeyPem) throws Exception {
        String normalized = publicKeyPem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] decoded = Base64.getDecoder().decode(normalized);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
        return KeyFactory.getInstance("RSA").generatePublic(keySpec);
    }

    public record TokenPair(
            String accessToken,
            String refreshToken,
            long accessExpiresInSeconds,
            long refreshExpiresInSeconds,
            String refreshJti,
            Instant refreshExpiresAt) {
    }

    public record ParsedRefreshToken(String subjectEmail, String tokenJti, String role, Instant expiresAt) {
    }
}
