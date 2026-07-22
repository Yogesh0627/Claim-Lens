-- =====================================================
-- V19__user_read_permission.sql
-- USER_READ: list back-office users (e.g. the investigator picker on claim assignment).
-- Granted to the roles that assign work or administer the tenant.
-- =====================================================

INSERT INTO permission (code, name, module) VALUES
    ('USER_READ', 'List tenant users', 'USER');

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.code IN ('TENANT_ADMIN', 'INVESTIGATION_MANAGER', 'CLAIMS_MANAGER')
  AND p.code = 'USER_READ';
