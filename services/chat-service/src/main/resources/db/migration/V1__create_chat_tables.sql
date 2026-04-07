-- Chat sessions: one per user, per "conversation"
CREATE TABLE chat_sessions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_email VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_chat_sessions_owner ON chat_sessions(owner_email);

-- Persistent chat messages (used for ratings lookup)
CREATE TABLE chat_messages (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id   UUID         NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    role         VARCHAR(10)  NOT NULL,   -- 'USER' or 'ASSISTANT'
    content      TEXT         NOT NULL,
    intent       VARCHAR(50),             -- classified intent string
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_chat_messages_session ON chat_messages(session_id, created_at DESC);

-- Per-message ratings (one rating per message)
CREATE TABLE chat_ratings (
    message_id  UUID NOT NULL PRIMARY KEY REFERENCES chat_messages(id) ON DELETE CASCADE,
    rating      VARCHAR(15) NOT NULL,    -- 'HELPFUL' or 'NOT_HELPFUL'
    rated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
