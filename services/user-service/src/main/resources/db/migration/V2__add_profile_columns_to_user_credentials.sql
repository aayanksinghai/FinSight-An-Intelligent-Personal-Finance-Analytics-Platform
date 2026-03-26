ALTER TABLE user_credentials ADD COLUMN full_name VARCHAR(120);
ALTER TABLE user_credentials ADD COLUMN city VARCHAR(120);
ALTER TABLE user_credentials ADD COLUMN age_group VARCHAR(40);
ALTER TABLE user_credentials ADD COLUMN monthly_income NUMERIC(14, 2);
ALTER TABLE user_credentials ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE;
