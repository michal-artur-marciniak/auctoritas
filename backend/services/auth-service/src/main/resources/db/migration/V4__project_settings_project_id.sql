-- Align project settings to be keyed by project
ALTER TABLE project_settings
  ADD COLUMN project_id UUID;

DELETE FROM project_settings
WHERE id NOT IN (
  SELECT settings_id
  FROM projects
  WHERE settings_id IS NOT NULL
);

UPDATE project_settings
SET project_id = projects.id
FROM projects
WHERE projects.settings_id = project_settings.id;

ALTER TABLE project_settings
  ALTER COLUMN project_id SET NOT NULL;

ALTER TABLE project_settings
  ADD CONSTRAINT uq_project_settings_project UNIQUE (project_id),
  ADD CONSTRAINT fk_project_settings_project
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE;

ALTER TABLE projects
  DROP COLUMN settings_id;
