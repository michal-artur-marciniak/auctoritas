ALTER TABLE project_settings
    ADD COLUMN oauth_config JSON NOT NULL DEFAULT '{}'::json;
