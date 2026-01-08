-- Projects (isolated environments per organization)
CREATE TABLE projects (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    slug            VARCHAR(50) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT uq_project_org_slug UNIQUE(organization_id, slug)
);

CREATE INDEX idx_projects_org ON projects(organization_id);

-- Project settings (password policy, MFA, OAuth, session config)
CREATE TABLE project_settings (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id                  UUID NOT NULL UNIQUE REFERENCES projects(id) ON DELETE CASCADE,
    
    -- Password policy
    password_min_length         INT NOT NULL DEFAULT 8,
    password_require_uppercase  BOOLEAN NOT NULL DEFAULT TRUE,
    password_require_lowercase  BOOLEAN NOT NULL DEFAULT TRUE,
    password_require_number     BOOLEAN NOT NULL DEFAULT TRUE,
    password_require_special    BOOLEAN NOT NULL DEFAULT FALSE,
    password_history_count      INT NOT NULL DEFAULT 0,
    
    -- MFA settings
    mfa_enabled                 BOOLEAN NOT NULL DEFAULT TRUE,
    mfa_required                BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- OAuth providers (JSONB for flexibility)
    oauth_google                JSONB,
    oauth_github                JSONB,
    oauth_microsoft             JSONB,
    oauth_apple                 JSONB,
    oauth_facebook              JSONB,
    
    -- Session settings
    access_token_ttl_seconds    INT NOT NULL DEFAULT 900,
    refresh_token_ttl_seconds   INT NOT NULL DEFAULT 604800,
    max_sessions_per_user       INT NOT NULL DEFAULT 5,
    
    -- General settings
    allow_registration          BOOLEAN NOT NULL DEFAULT TRUE,
    require_email_verification  BOOLEAN NOT NULL DEFAULT TRUE,
    
    created_at                  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- API keys for SDK authentication
CREATE TABLE api_keys (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    key_hash        VARCHAR(255) NOT NULL UNIQUE,
    key_prefix      VARCHAR(20) NOT NULL,
    last_used_at    TIMESTAMP,
    expires_at      TIMESTAMP,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_api_keys_project ON api_keys(project_id);
CREATE INDEX idx_api_keys_prefix ON api_keys(key_prefix);
CREATE INDEX idx_api_keys_status ON api_keys(status);
