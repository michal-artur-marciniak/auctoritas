-- Add optional setting to require verified email for end-user login
ALTER TABLE project_settings
  ADD COLUMN IF NOT EXISTS require_verified_email_for_login BOOLEAN NOT NULL DEFAULT FALSE;
