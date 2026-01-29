-- Project settings (password policy, MFA, session config)
CREATE TABLE project_settings (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Password policy
    min_length                  INT NOT NULL DEFAULT 8,
    require_uppercase           BOOLEAN NOT NULL DEFAULT TRUE,
    require_lowercase           BOOLEAN NOT NULL DEFAULT TRUE,
    require_numbers             BOOLEAN NOT NULL DEFAULT TRUE,
    require_special_chars       BOOLEAN NOT NULL DEFAULT FALSE,
    password_history_count      INT NOT NULL DEFAULT 0,
    
    -- Session settings
    access_token_ttl_seconds    INT NOT NULL DEFAULT 3600,
    refresh_token_ttl_seconds   INT NOT NULL DEFAULT 604800,
    max_sessions                INT NOT NULL DEFAULT 5,
    
    -- MFA settings
    mfa_enabled                 BOOLEAN NOT NULL DEFAULT FALSE,
    mfa_required                BOOLEAN NOT NULL DEFAULT FALSE,
    
    created_at                  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Projects (isolated environments per organization)
CREATE TABLE projects (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    slug            VARCHAR(50) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    settings_id     UUID REFERENCES project_settings(id) ON DELETE SET NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT uq_project_org_slug UNIQUE(organization_id, slug)
);

CREATE INDEX idx_projects_org ON projects(organization_id);

-- API keys for SDK authentication
CREATE TABLE api_keys (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name            VARCHAR(50) NOT NULL,
    prefix          VARCHAR(10) NOT NULL,
    key_hash        VARCHAR(64) NOT NULL UNIQUE,
    last_used_at    TIMESTAMP,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_api_keys_project ON api_keys(project_id);
CREATE INDEX idx_api_keys_prefix ON api_keys(prefix);
CREATE INDEX idx_api_keys_status ON api_keys(status);
