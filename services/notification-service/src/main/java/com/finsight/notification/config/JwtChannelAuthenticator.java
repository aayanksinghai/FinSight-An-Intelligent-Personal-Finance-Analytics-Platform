package com.finsight.notification.config;

import com.finsight.notification.security.JwtPrincipal;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Validates a raw JWT string and returns a JwtPrincipal.
 * Used by the STOMP ChannelInterceptor so WebSocket sessions are tied to a user email.
 */
@Component
public class JwtChannelAuthenticator {

    private static final Logger log = LoggerFactory.getLogger(JwtChannelAuthenticator.class);

    private final JwtDecoder jwtDecoder;

    public JwtChannelAuthenticator(@Value("${security.jwt.public-key}") String publicKeyPem) throws Exception {
        String pem = publicKeyPem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] decoded = Base64.getDecoder().decode(pem);
        RSAPublicKey rsaPublicKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(decoded));
        this.jwtDecoder = NimbusJwtDecoder.withPublicKey(rsaPublicKey).build();
    }

    /**
     * Validates the token and extracts the user's email from the 'sub' claim.
     *
     * @throws IllegalArgumentException if the token is invalid or expired
     */
    public JwtPrincipal authenticate(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            String email = jwt.getSubject();
            if (email == null || email.isBlank()) {
                throw new IllegalArgumentException("JWT sub claim is missing");
            }
            return new JwtPrincipal(email);
        } catch (JwtException e) {
            log.warn("Invalid JWT on STOMP CONNECT: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid or expired JWT", e);
        }
    }
}
