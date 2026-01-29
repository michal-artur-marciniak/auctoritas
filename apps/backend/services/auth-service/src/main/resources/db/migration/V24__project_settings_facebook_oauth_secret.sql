-- Store Facebook OAuth client secret encrypted at rest
ALTER TABLE project_settings
  ADD COLUMN IF NOT EXISTS oauth_facebook_client_secret_enc TEXT;
