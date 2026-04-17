-- Add title column to notifications for richer display
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS title VARCHAR(200);

-- User notification preferences: which types they want to receive
CREATE TABLE IF NOT EXISTS notification_preferences (
    owner_email VARCHAR(255) NOT NULL,
    type        VARCHAR(50)  NOT NULL,
    enabled     BOOLEAN      NOT NULL DEFAULT true,
    PRIMARY KEY (owner_email, type)
);

CREATE INDEX IF NOT EXISTS idx_notif_prefs_email ON notification_preferences(owner_email);
