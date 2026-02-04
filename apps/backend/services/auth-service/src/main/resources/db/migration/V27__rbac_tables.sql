-- RBAC tables for project-scoped roles and permissions

CREATE TABLE roles (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  project_id  UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
  name        VARCHAR(50) NOT NULL,
  description VARCHAR(255),
  is_system   BOOLEAN NOT NULL DEFAULT FALSE,
  created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),

  CONSTRAINT uq_roles_project_name UNIQUE(project_id, name)
);

CREATE INDEX idx_roles_project ON roles(project_id);

CREATE TABLE permissions (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  code        VARCHAR(100) NOT NULL UNIQUE,
  description VARCHAR(255) NOT NULL,
  category    VARCHAR(50) NOT NULL,
  created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_permissions_code ON permissions(code);

CREATE TABLE role_permissions (
  role_id       UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
  permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
  created_at    TIMESTAMP NOT NULL DEFAULT NOW(),

  CONSTRAINT pk_role_permissions PRIMARY KEY (role_id, permission_id)
);

CREATE INDEX idx_role_permissions_role ON role_permissions(role_id);
CREATE INDEX idx_role_permissions_permission ON role_permissions(permission_id);

CREATE TABLE user_roles (
  user_id    UUID NOT NULL REFERENCES end_users(id) ON DELETE CASCADE,
  role_id    UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),

  CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_user_roles_user ON user_roles(user_id);
CREATE INDEX idx_user_roles_role ON user_roles(role_id);

INSERT INTO permissions (id, code, description, category, created_at)
SELECT data.id, data.code, data.description, data.category, data.created_at
FROM (
  VALUES
    (gen_random_uuid(), 'users:read', 'Read end users', 'users', NOW()),
    (gen_random_uuid(), 'users:write', 'Create and update end users', 'users', NOW()),
    (gen_random_uuid(), 'users:delete', 'Delete end users', 'users', NOW()),
    (gen_random_uuid(), 'roles:read', 'Read roles', 'roles', NOW()),
    (gen_random_uuid(), 'roles:write', 'Create and update roles', 'roles', NOW()),
    (gen_random_uuid(), 'roles:delete', 'Delete roles', 'roles', NOW())
) AS data(id, code, description, category, created_at)
WHERE NOT EXISTS (
  SELECT 1 FROM permissions p WHERE p.code = data.code
);
