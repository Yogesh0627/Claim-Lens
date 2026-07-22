-- =====================================================
-- V5__permission_seed.sql
-- Global permissions + role_permission mappings (roles/permissions are global; no tenant_id).
-- Without this, every @PreAuthorize denies (V3 seeded roles only).
-- Roles are referenced by code, never by generated id.
-- =====================================================

INSERT INTO permission (code, name, module) VALUES
    ('ORG_COMPANY_READ',      'Read companies',      'ORGANIZATION'),
    ('ORG_COMPANY_WRITE',     'Manage companies',    'ORGANIZATION'),
    ('ORG_REGION_READ',       'Read regions',        'ORGANIZATION'),
    ('ORG_REGION_WRITE',      'Manage regions',      'ORGANIZATION'),
    ('ORG_BRANCH_READ',       'Read branches',       'ORGANIZATION'),
    ('ORG_BRANCH_WRITE',      'Manage branches',     'ORGANIZATION'),
    ('ORG_DEPARTMENT_READ',   'Read departments',    'ORGANIZATION'),
    ('ORG_DEPARTMENT_WRITE',  'Manage departments',  'ORGANIZATION'),
    ('ORG_DESIGNATION_READ',  'Read designations',   'ORGANIZATION'),
    ('ORG_DESIGNATION_WRITE', 'Manage designations', 'ORGANIZATION');

-- TENANT_ADMIN: full organization access.
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r, permission p
WHERE r.code = 'TENANT_ADMIN'
  AND p.module = 'ORGANIZATION';

-- AUDITOR: read-only organization access.
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r, permission p
WHERE r.code = 'AUDITOR'
  AND p.code LIKE 'ORG\_%\_READ';
