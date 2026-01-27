-- Store GitHub OAuth client secret encrypted at rest
ALTER TABLE project_settings
  ADD COLUMN IF NOT EXISTS oauth_github_client_secret_enc TEXT;
