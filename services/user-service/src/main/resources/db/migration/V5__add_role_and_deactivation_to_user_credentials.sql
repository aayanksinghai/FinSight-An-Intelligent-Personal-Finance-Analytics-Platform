ALTER TABLE user_credentials ADD COLUMN role VARCHAR(20);
UPDATE user_credentials SET role = 'USER' WHERE role IS NULL;
ALTER TABLE user_credentials ALTER COLUMN role SET NOT NULL;
ALTER TABLE user_credentials ADD COLUMN deactivated_at TIMESTAMP WITH TIME ZONE;

