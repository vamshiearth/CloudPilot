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
    ('emma@megacorp.test', crypt('CloudPilot123!', gen_salt('bf', 10)), 'Emma', 'Stone', 'ACTIVE', NOW(), NOW()),
    ('maya@northstar.test', crypt('CloudPilot123!', gen_salt('bf', 10)), 'Maya', 'Patel', 'ACTIVE', NOW(), NOW()),
    ('liam@redwood.test', crypt('CloudPilot123!', gen_salt('bf', 10)), 'Liam', 'Brooks', 'ACTIVE', NOW(), NOW()),
    ('olivia@orbit.test', crypt('CloudPilot123!', gen_salt('bf', 10)), 'Olivia', 'Chen', 'ACTIVE', NOW(), NOW()),
    ('sarah@acme.test', crypt('CloudPilot123!', gen_salt('bf', 10)), 'Sarah', 'Miller', 'ACTIVE', NOW(), NOW()),
    ('noah@northstar.test', crypt('CloudPilot123!', gen_salt('bf', 10)), 'Noah', 'Williams', 'ACTIVE', NOW(), NOW())
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
    ('MegaCorp', 'megacorp', 'FREE'),
    ('Northstar Labs', 'northstar-labs', 'STARTER'),
    ('Redwood Finance', 'redwood-finance', 'PRO'),
    ('Orbit Health', 'orbit-health', 'STARTER')
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
WHERE tenant.slug IN ('acme-corporation', 'megacorp', 'northstar-labs', 'redwood-finance', 'orbit-health')
ON CONFLICT (tenant_id, name) DO UPDATE SET description = EXCLUDED.description;

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM roles role
JOIN tenants tenant ON tenant.id = role.tenant_id
CROSS JOIN permissions permission
WHERE tenant.slug IN ('acme-corporation', 'megacorp', 'northstar-labs', 'redwood-finance', 'orbit-health')
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
    ('megacorp', 'emma@megacorp.test', 'OWNER'),
    ('northstar-labs', 'maya@northstar.test', 'OWNER'),
    ('redwood-finance', 'liam@redwood.test', 'OWNER'),
    ('orbit-health', 'olivia@orbit.test', 'OWNER'),
    ('acme-corporation', 'sarah@acme.test', 'MEMBER'),
    ('northstar-labs', 'noah@northstar.test', 'MEMBER')
) AS seed(tenant_slug, email, role_name)
JOIN tenants tenant ON tenant.slug = seed.tenant_slug
JOIN users app_user ON app_user.email = seed.email
JOIN roles role ON role.tenant_id = tenant.id AND role.name = seed.role_name
ON CONFLICT (tenant_id, user_id) DO UPDATE SET
    role_id = EXCLUDED.role_id,
    status = EXCLUDED.status;

INSERT INTO projects (tenant_id, name, description, status, created_by, created_at, updated_at)
SELECT tenant.id, seed.project_name, seed.description, 'ACTIVE', app_user.id, NOW(), NOW()
FROM (VALUES
    ('acme-corporation', 'Customer Portal', 'Build the customer self-service portal', 'john@acme.test'),
    ('acme-corporation', 'Billing Refresh', 'Improve billing and subscription workflows', 'john@acme.test'),
    ('megacorp', 'Data Migration', 'Move legacy records into CloudPilot', 'emma@megacorp.test'),
    ('megacorp', 'Operations Dashboard', 'Create operational reporting views', 'emma@megacorp.test'),
    ('northstar-labs', 'Research Workspace', 'Coordinate research experiments', 'maya@northstar.test'),
    ('northstar-labs', 'Release Automation', 'Automate release validation', 'maya@northstar.test'),
    ('redwood-finance', 'Risk Analytics', 'Build portfolio risk analysis', 'liam@redwood.test'),
    ('redwood-finance', 'Compliance Review', 'Track compliance evidence', 'liam@redwood.test'),
    ('orbit-health', 'Patient Scheduling', 'Improve appointment scheduling', 'olivia@orbit.test'),
    ('orbit-health', 'Provider Directory', 'Maintain provider information', 'olivia@orbit.test')
) AS seed(tenant_slug, project_name, description, owner_email)
JOIN tenants tenant ON tenant.slug = seed.tenant_slug
JOIN users app_user ON app_user.email = seed.owner_email
WHERE NOT EXISTS (
    SELECT 1 FROM projects existing
    WHERE existing.tenant_id = tenant.id AND existing.name = seed.project_name
);

INSERT INTO tasks
    (tenant_id, project_id, title, description, status, priority, assigned_to, created_by, due_date, created_at, updated_at)
SELECT tenant.id, project.id, seed.title, seed.description, seed.status, seed.priority,
       assignee.id, creator.id, NOW() + seed.due_in, NOW(), NOW()
FROM (VALUES
    ('acme-corporation', 'Customer Portal', 'Design account page', 'Create account settings layout', 'TODO', 'HIGH', 'sarah@acme.test', 'john@acme.test', INTERVAL '5 days'),
    ('acme-corporation', 'Billing Refresh', 'Verify Redis cache', 'Validate subscription cache behavior', 'IN_PROGRESS', 'HIGH', 'john@acme.test', 'john@acme.test', INTERVAL '2 days'),
    ('megacorp', 'Data Migration', 'Map legacy fields', 'Document source-to-target mappings', 'TODO', 'MEDIUM', NULL, 'emma@megacorp.test', INTERVAL '7 days'),
    ('megacorp', 'Operations Dashboard', 'Define KPIs', 'Choose dashboard operational metrics', 'DONE', 'LOW', 'emma@megacorp.test', 'emma@megacorp.test', INTERVAL '1 day'),
    ('northstar-labs', 'Research Workspace', 'Prepare experiment', 'Set up the next experiment workspace', 'IN_PROGRESS', 'HIGH', 'noah@northstar.test', 'maya@northstar.test', INTERVAL '3 days'),
    ('northstar-labs', 'Release Automation', 'Add smoke tests', 'Automate post-deployment checks', 'TODO', 'MEDIUM', 'maya@northstar.test', 'maya@northstar.test', INTERVAL '6 days'),
    ('redwood-finance', 'Risk Analytics', 'Review exposure model', 'Validate model assumptions', 'TODO', 'HIGH', 'liam@redwood.test', 'liam@redwood.test', INTERVAL '4 days'),
    ('redwood-finance', 'Compliance Review', 'Collect audit evidence', 'Attach current compliance evidence', 'IN_PROGRESS', 'MEDIUM', 'liam@redwood.test', 'liam@redwood.test', INTERVAL '8 days'),
    ('orbit-health', 'Patient Scheduling', 'Test appointment flow', 'Validate booking and cancellation', 'TODO', 'HIGH', 'olivia@orbit.test', 'olivia@orbit.test', INTERVAL '5 days'),
    ('orbit-health', 'Provider Directory', 'Import provider list', 'Load the initial provider directory', 'DONE', 'LOW', 'olivia@orbit.test', 'olivia@orbit.test', INTERVAL '1 day')
) AS seed(tenant_slug, project_name, title, description, status, priority, assignee_email, creator_email, due_in)
JOIN tenants tenant ON tenant.slug = seed.tenant_slug
JOIN projects project ON project.tenant_id = tenant.id AND project.name = seed.project_name
JOIN users creator ON creator.email = seed.creator_email
LEFT JOIN users assignee ON assignee.email = seed.assignee_email
WHERE NOT EXISTS (
    SELECT 1 FROM tasks existing
    WHERE existing.project_id = project.id AND existing.title = seed.title
);

INSERT INTO tenant_invitations
    (tenant_id, email, role_id, invited_by, token, status, expires_at, created_at, accepted_at)
SELECT tenant.id, seed.email, role.id, inviter.id, seed.token::uuid::text,
       'PENDING', NOW() + INTERVAL '7 days', NOW(), NULL
FROM (VALUES
    ('acme-corporation', 'invitee1@acme.test', 'john@acme.test', '11111111-1111-4111-8111-111111111111'),
    ('megacorp', 'invitee2@megacorp.test', 'emma@megacorp.test', '22222222-2222-4222-8222-222222222222'),
    ('northstar-labs', 'invitee3@northstar.test', 'maya@northstar.test', '33333333-3333-4333-8333-333333333333'),
    ('redwood-finance', 'invitee4@redwood.test', 'liam@redwood.test', '44444444-4444-4444-8444-444444444444'),
    ('orbit-health', 'invitee5@orbit.test', 'olivia@orbit.test', '55555555-5555-4555-8555-555555555555')
) AS seed(tenant_slug, email, inviter_email, token)
JOIN tenants tenant ON tenant.slug = seed.tenant_slug
JOIN roles role ON role.tenant_id = tenant.id AND role.name = 'MEMBER'
JOIN users inviter ON inviter.email = seed.inviter_email
ON CONFLICT (token) DO UPDATE SET
    email = EXCLUDED.email,
    role_id = EXCLUDED.role_id,
    invited_by = EXCLUDED.invited_by,
    status = EXCLUDED.status,
    expires_at = EXCLUDED.expires_at,
    accepted_at = NULL;

COMMIT;

SELECT 'users' AS table_name, COUNT(*) AS row_count FROM users
UNION ALL SELECT 'tenants', COUNT(*) FROM tenants
UNION ALL SELECT 'tenant_memberships', COUNT(*) FROM tenant_memberships
UNION ALL SELECT 'tenant_invitations', COUNT(*) FROM tenant_invitations
UNION ALL SELECT 'projects', COUNT(*) FROM projects
UNION ALL SELECT 'tasks', COUNT(*) FROM tasks
UNION ALL SELECT 'subscription_plans', COUNT(*) FROM subscription_plans
UNION ALL SELECT 'subscription_features', COUNT(*) FROM subscription_features
UNION ALL SELECT 'subscription_plan_features', COUNT(*) FROM subscription_plan_features
UNION ALL SELECT 'roles', COUNT(*) FROM roles
UNION ALL SELECT 'permissions', COUNT(*) FROM permissions
UNION ALL SELECT 'role_permissions', COUNT(*) FROM role_permissions
ORDER BY table_name;