CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY,
    token VARCHAR(128) NOT NULL,
    user_email VARCHAR(320) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX uk_password_reset_tokens_token ON password_reset_tokens (token);
CREATE INDEX idx_password_reset_tokens_user_email ON password_reset_tokens (user_email);

