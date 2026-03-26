package com.finsight.user.security;

import com.finsight.user.persistence.RevokedAccessTokenRepository;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class AccessTokenValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INVALID_TOKEN =
            new OAuth2Error("invalid_token", "Token is invalid or revoked", null);

    private final RevokedAccessTokenRepository revokedAccessTokenRepository;

    public AccessTokenValidator(RevokedAccessTokenRepository revokedAccessTokenRepository) {
        this.revokedAccessTokenRepository = revokedAccessTokenRepository;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        Object type = token.getClaims().get("typ");
        if (!"access".equals(type)) {
            return OAuth2TokenValidatorResult.failure(INVALID_TOKEN);
        }

        if (token.getId() != null && revokedAccessTokenRepository.existsById(token.getId())) {
            return OAuth2TokenValidatorResult.failure(INVALID_TOKEN);
        }

        return OAuth2TokenValidatorResult.success();
    }
}

