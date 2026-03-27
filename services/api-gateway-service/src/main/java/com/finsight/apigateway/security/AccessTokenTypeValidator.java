package com.finsight.apigateway.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class AccessTokenTypeValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INVALID_TOKEN =
            new OAuth2Error("invalid_token", "Only access tokens are accepted for protected APIs", null);

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        Object tokenType = token.getClaims().get("typ");
        if (!"access".equals(tokenType)) {
            return OAuth2TokenValidatorResult.failure(INVALID_TOKEN);
        }
        return OAuth2TokenValidatorResult.success();
    }
}

