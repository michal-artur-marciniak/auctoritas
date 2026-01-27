-- Short-lived OAuth authorization requests (state + PKCE verifier + app redirect URI)
CREATE TABLE oauth_authorization_requests (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  project_id       UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
  provider         VARCHAR(50) NOT NULL,
  state_hash       VARCHAR(128) NOT NULL,
  code_verifier    VARCHAR(256) NOT NULL,
  app_redirect_uri VARCHAR(2000) NOT NULL,
  expires_at       TIMESTAMPTZ NOT NULL,
  created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  CONSTRAINT uq_oauth_authorization_requests_state_hash UNIQUE(state_hash)
);

CREATE INDEX idx_oauth_authorization_requests_project ON oauth_authorization_requests(project_id);
CREATE INDEX idx_oauth_authorization_requests_provider ON oauth_authorization_requests(provider);
CREATE INDEX idx_oauth_authorization_requests_expires_at ON oauth_authorization_requests(expires_at);
