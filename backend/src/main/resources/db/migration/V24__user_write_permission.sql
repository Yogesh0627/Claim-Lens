-- =====================================================
-- V24__user_write_permission.sql
-- USER_WRITE: create / edit / deactivate back-office users. A tenant-admin concern.
-- =====================================================

INSERT INTO permission (code, name, module) VALUES
    ('USER_WRITE', 'Manage tenant users', 'USER');

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.code = 'TENANT_ADMIN' AND p.code = 'USER_WRITE';
