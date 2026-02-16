-- Push Notifications Database Migration
-- Run this on your database to add FCM token fields to the users table

-- Add FCM token field (max 500 characters for long Firebase tokens)
ALTER TABLE users ADD COLUMN fcm_token VARCHAR(500);

-- Add last token update timestamp
ALTER TABLE users ADD COLUMN last_token_update TIMESTAMP;

-- Optional: Add index for faster lookups
CREATE INDEX idx_users_fcm_token ON users(fcm_token);

-- Verify changes
DESCRIBE users;
