-- Store Google OAuth client secret encrypted at rest
ALTER TABLE project_settings
  ADD COLUMN IF NOT EXISTS oauth_google_client_secret_enc TEXT;
