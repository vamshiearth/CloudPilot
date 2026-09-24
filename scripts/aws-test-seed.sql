BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO subscription_features (name, display_name, description)
VALUES ('ADVANCED_RBAC', 'Advanced RBAC', 'Allows organization administrators to change member roles.')
ON CONFLICT (name) DO UPDATE SET
    display_name = EXCLUDED.display_name,
    description = EXCLUDED.description;

INSERT INTO subscription_plans
    (name, display_name, description, active, max_members, max_projects, created_at, updated_at)
VALUES
    ('FREE', 'Free', 'Basic CloudPilot plan', TRUE, 3, 3, NOW(), NOW()),
    ('STARTER', 'Starter', 'Plan for growing teams', TRUE, 10, 20, NOW(), NOW()),
    ('PRO', 'Pro', 'Advanced CloudPilot plan', TRUE, 50, 100, NOW(), NOW())
ON CONFLICT (name) DO UPDATE SET
    display_name = EXCLUDED.display_name,
    description = EXCLUDED.description,
    active = EXCLUDED.active,
    max_members = EXCLUDED.max_members,
    max_projects = EXCLUDED.max_projects,
    updated_at = NOW();

INSERT INTO subscription_plan_features (plan_id, feature_id)
SELECT plan.id, feature.id
FROM subscription_plans plan
CROSS JOIN subscription_features feature
WHERE plan.name IN ('STARTER', 'PRO')
  AND feature.name = 'ADVANCED_RBAC'
ON CONFLICT (plan_id, feature_id) DO NOTHING;

INSERT INTO permissions (name, description)
VALUES
    ('PROJECT_CREATE', 'project create'),
    ('PROJECT_READ', 'project read'),
    ('PROJECT_UPDATE', 'project update'),
    ('PROJECT_DELETE', 'project delete'),
    ('TASK_CREATE', 'task create'),
    ('TASK_READ', 'task read'),
    ('TASK_UPDATE', 'task update'),
    ('TASK_DELETE', 'task delete'),
    ('USER_READ', 'user read'),
    ('USER_INVITE', 'user invite'),
    ('USER_REMOVE', 'user remove'),
    ('ROLE_READ', 'role read'),
    ('ROLE_ASSIGN', 'role assign'),
    ('BILLING_READ', 'billing read'),
    ('BILLING_UPDATE', 'billing update'),
    ('SETTINGS_READ', 'settings read'),
    ('SETTINGS_UPDATE', 'settings update')
ON CONFLICT (name) DO UPDATE SET description = EXCLUDED.description;

INSERT INTO users
    (email, password_hash, first_name, last_name, status, created_at, updated_at)
VALUES
    ('john@acme.test', crypt('CloudPilot123!', gen_salt('bf', 10)), 'John', 'Carter', 'ACTIVE', NOW(), NOW()),
    ('sarah@acme.test', crypt('CloudPilot123!', gen_salt('bf', 10)), 'Sarah', 'Miller', 'ACTIVE', NOW(), NOW()),
    ('emma@megacorp.test', crypt('CloudPilot123!', gen_salt('bf', 10)), 'Emma', 'Stone', 'ACTIVE', NOW(), NOW())
ON CONFLICT (email) DO UPDATE SET
    password_hash = EXCLUDED.password_hash,
    first_name = EXCLUDED.first_name,
    last_name = EXCLUDED.last_name,
    status = EXCLUDED.status,
    updated_at = NOW();

INSERT INTO tenants (name, slug, status, plan_id, created_at, updated_at)
SELECT seed.name, seed.slug, 'ACTIVE', plan.id, NOW(), NOW()
FROM (VALUES
    ('Acme Corporation', 'acme-corporation', 'FREE'),
    ('MegaCorp', 'megacorp', 'FREE')
) AS seed(name, slug, plan_name)
JOIN subscription_plans plan ON plan.name = seed.plan_name
ON CONFLICT (slug) DO UPDATE SET
    name = EXCLUDED.name,
    status = EXCLUDED.status,
    plan_id = EXCLUDED.plan_id,
    updated_at = NOW();

INSERT INTO roles (tenant_id, name, description)
SELECT tenant.id, role.name, role.description
FROM tenants tenant
CROSS JOIN (VALUES
    ('OWNER', 'Full organization access'),
    ('ADMIN', 'Manage users, roles, projects, tasks, and settings'),
    ('MEMBER', 'Standard project and task access')
) AS role(name, description)
WHERE tenant.slug IN ('acme-corporation', 'megacorp')
ON CONFLICT (tenant_id, name) DO UPDATE SET description = EXCLUDED.description;

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM roles role
JOIN tenants tenant ON tenant.id = role.tenant_id
CROSS JOIN permissions permission
WHERE tenant.slug IN ('acme-corporation', 'megacorp')
  AND (
      role.name = 'OWNER'
      OR role.name = 'ADMIN' AND permission.name IN (
          'PROJECT_CREATE', 'PROJECT_READ', 'PROJECT_UPDATE', 'PROJECT_DELETE',
          'TASK_CREATE', 'TASK_READ', 'TASK_UPDATE', 'TASK_DELETE',
          'USER_READ', 'USER_INVITE', 'USER_REMOVE', 'ROLE_READ', 'ROLE_ASSIGN',
          'SETTINGS_READ', 'SETTINGS_UPDATE'
      )
      OR role.name = 'MEMBER' AND permission.name IN (
          'PROJECT_READ', 'TASK_CREATE', 'TASK_READ', 'TASK_UPDATE'
      )
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO tenant_memberships (tenant_id, user_id, role_id, status, joined_at)
SELECT tenant.id, app_user.id, role.id, 'ACTIVE', NOW()
FROM (VALUES
    ('acme-corporation', 'john@acme.test', 'OWNER'),
    ('acme-corporation', 'sarah@acme.test', 'MEMBER'),
    ('megacorp', 'emma@megacorp.test', 'OWNER')
) AS seed(tenant_slug, email, role_name)
JOIN tenants tenant ON tenant.slug = seed.tenant_slug
JOIN users app_user ON app_user.email = seed.email
JOIN roles role ON role.tenant_id = tenant.id AND role.name = seed.role_name
ON CONFLICT (tenant_id, user_id) DO UPDATE SET
    role_id = EXCLUDED.role_id,
    status = EXCLUDED.status;

COMMIT;
