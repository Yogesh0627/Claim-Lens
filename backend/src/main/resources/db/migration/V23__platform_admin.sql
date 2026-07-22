-- =====================================================
-- V23__platform_admin.sql
-- PLATFORM_ADMIN: the SaaS operator that sits ABOVE the tenants (distinct from a tenant admin, who
-- only runs their own insurance company). Global role + permission (role/permission are global).
-- The PLATFORM_ADMIN permission gates the cross-tenant /platform endpoints; company management is
-- also granted here (onboarding/suspending tenants is a platform concern).
-- Cross-tenant *data* access is delivered via impersonation (a tenant-scoped token issued by the
-- platform endpoint), so no @TenantId query is ever bypassed for writes.
-- =====================================================

INSERT INTO permission (code, name, module) VALUES
    ('PLATFORM_ADMIN', 'Platform administration (cross-tenant)', 'PLATFORM');

INSERT INTO role (code, name, description, is_system_role, status) VALUES
    ('PLATFORM_ADMIN', 'Platform Admin',
     'ClaimLens platform operator — manages tenants and sees cross-tenant analytics', TRUE, 'ACTIVE');

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.code = 'PLATFORM_ADMIN'
  AND p.code IN ('PLATFORM_ADMIN', 'ORG_COMPANY_READ', 'ORG_COMPANY_WRITE');
