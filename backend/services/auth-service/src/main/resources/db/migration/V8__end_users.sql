-- End users (application users per project)
CREATE TABLE end_users (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  project_id      UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
  email           VARCHAR(255) NOT NULL,
  password_hash   VARCHAR(255) NOT NULL,
  name            VARCHAR(100),
  email_verified  BOOLEAN NOT NULL DEFAULT FALSE,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  CONSTRAINT uq_end_user_email UNIQUE(project_id, email)
);

CREATE INDEX idx_end_users_project ON end_users(project_id);
CREATE INDEX idx_end_users_email ON end_users(email);

-- Sessions for end users
CREATE TABLE end_user_sessions (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id         UUID NOT NULL REFERENCES end_users(id) ON DELETE CASCADE,
  device_info     JSONB,
  ip_address      VARCHAR(45),
  expires_at      TIMESTAMPTZ NOT NULL,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_end_user_sessions_user ON end_user_sessions(user_id);
CREATE INDEX idx_end_user_sessions_expires ON end_user_sessions(expires_at);

-- Refresh tokens for end users
CREATE TABLE end_user_refresh_tokens (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id         UUID NOT NULL REFERENCES end_users(id) ON DELETE CASCADE,
  token_hash      VARCHAR(128) NOT NULL UNIQUE,
  expires_at      TIMESTAMPTZ NOT NULL,
  revoked         BOOLEAN NOT NULL DEFAULT FALSE,
  replaced_by_id  UUID REFERENCES end_user_refresh_tokens(id) ON DELETE SET NULL,
  ip_address      VARCHAR(45),
  user_agent      VARCHAR(500),
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_end_user_refresh_tokens_user ON end_user_refresh_tokens(user_id);
CREATE INDEX idx_end_user_refresh_tokens_expires ON end_user_refresh_tokens(expires_at);
