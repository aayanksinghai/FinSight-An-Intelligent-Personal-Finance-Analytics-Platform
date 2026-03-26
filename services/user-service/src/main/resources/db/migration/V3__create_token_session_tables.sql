CREATE TABLE refresh_token_sessions (
    id UUID PRIMARY KEY,
    token_jti VARCHAR(64) NOT NULL,
    user_email VARCHAR(320) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    revoked_at TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX uk_refresh_token_sessions_jti ON refresh_token_sessions (token_jti);
CREATE INDEX idx_refresh_token_sessions_user_email ON refresh_token_sessions (user_email);

CREATE TABLE revoked_access_tokens (
    token_jti VARCHAR(64) PRIMARY KEY,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);

