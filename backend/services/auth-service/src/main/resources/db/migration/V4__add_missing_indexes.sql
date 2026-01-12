-- Add missing database indexes for optimized queries

-- Composite index for org member login lookups (email + org_id)
-- This is more efficient than separate indexes for email lookups within org context
CREATE INDEX IF NOT EXISTS idx_org_members_email_org
    ON organization_members(email, organization_id);

-- Index for API key validation by hashed key
-- Required for fast API key lookups during SDK authentication
CREATE INDEX IF NOT EXISTS idx_api_keys_hashed_key
    ON api_keys(key_hash);
