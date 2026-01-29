-- Store Apple Sign-In private key encrypted at rest
ALTER TABLE project_settings
  ADD COLUMN IF NOT EXISTS oauth_apple_private_key_enc TEXT;
