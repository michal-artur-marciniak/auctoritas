-- One-time OAuth exchange codes (redirect -> token exchange)
CREATE TABLE oauth_exchange_codes (
  id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
  user_id    UUID NOT NULL REFERENCES end_users(id) ON DELETE CASCADE,
  provider   VARCHAR(50) NOT NULL,
  code_hash  VARCHAR(128) NOT NULL UNIQUE,
  expires_at TIMESTAMPTZ NOT NULL,
  used_at    TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_oauth_exchange_codes_project_user ON oauth_exchange_codes(project_id, user_id);
CREATE INDEX idx_oauth_exchange_codes_user ON oauth_exchange_codes(user_id);
CREATE INDEX idx_oauth_exchange_codes_expires ON oauth_exchange_codes(expires_at);
