-- OAuth connections between end users and external identity providers
CREATE TABLE oauth_connections (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  project_id        UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
  user_id           UUID NOT NULL REFERENCES end_users(id) ON DELETE CASCADE,
  provider          VARCHAR(50) NOT NULL,
  provider_user_id  VARCHAR(255) NOT NULL,
  email             VARCHAR(255) NOT NULL,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  CONSTRAINT uq_oauth_connections_provider_user UNIQUE(project_id, provider, provider_user_id)
);

CREATE INDEX idx_oauth_connections_project ON oauth_connections(project_id);
CREATE INDEX idx_oauth_connections_user ON oauth_connections(user_id);
CREATE INDEX idx_oauth_connections_provider ON oauth_connections(provider);
CREATE INDEX idx_oauth_connections_email ON oauth_connections(email);
