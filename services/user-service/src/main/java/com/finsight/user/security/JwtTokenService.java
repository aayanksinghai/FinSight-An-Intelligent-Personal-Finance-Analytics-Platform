package com.finsight.user.security;

import io.jsonwebtoken.Jwts;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private final PrivateKey privateKey;
    private final String issuer;
    private final long expirySeconds;

    public JwtTokenService(
            @Value("${security.jwt.private-key}") String privateKeyPem,
            @Value("${security.jwt.issuer}") String issuer,
            @Value("${security.jwt.expiry-seconds}") long expirySeconds) throws Exception {
        this.privateKey = parsePrivateKey(privateKeyPem);
        this.issuer = issuer;
        this.expirySeconds = expirySeconds;
    }

    public String issueToken(String subjectEmail) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subjectEmail)
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirySeconds)))
                .claim("role", "USER")
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    public long getExpirySeconds() {
        return expirySeconds;
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
}

