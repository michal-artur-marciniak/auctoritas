ALTER TABLE api_keys
  ADD CONSTRAINT uk_api_keys_project_name UNIQUE (project_id, name);
