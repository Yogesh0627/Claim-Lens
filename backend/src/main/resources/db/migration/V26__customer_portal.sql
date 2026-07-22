-- =====================================================
-- V26__customer_portal.sql
-- Customer self-service portal. Adds a CUSTOMER role whose permissions are DISJOINT from every staff
-- role: PORTAL_* codes only. A customer therefore cannot call any staff endpoint (those require
-- CLAIM_READ/POLICY_READ, which CUSTOMER lacks) — they can reach only the ownership-scoped /portal/*
-- surface. The link from a login to a policyholder is app_user.customer_id.
-- =====================================================

-- The self-service role. Not a "system" back-office role, but global like all roles (D12).
INSERT INTO role (code, name, description, is_system_role, status) VALUES
    ('CUSTOMER', 'Customer', 'Policyholder self-service: file and track own claims.', TRUE, 'ACTIVE');

-- Portal permissions live in their own module so they can never be swept into a staff role by a
-- module-wide grant (e.g. the TENANT_ADMIN "all ORGANIZATION" pattern).
INSERT INTO permission (code, name, module) VALUES
    ('PORTAL_CLAIM_READ',  'Read own claims',            'PORTAL'),
    ('PORTAL_CLAIM_WRITE', 'File / submit own claims',   'PORTAL'),
    ('PORTAL_POLICY_READ', 'Read own policies',          'PORTAL');

-- CUSTOMER gets every PORTAL permission and nothing else.
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r, permission p
WHERE r.code = 'CUSTOMER'
  AND p.module = 'PORTAL';

-- A login account for a customer points at their customer row. NULL for every staff user.
ALTER TABLE app_user ADD COLUMN customer_id BIGINT;
ALTER TABLE app_user ADD CONSTRAINT fk_app_user_customer
    FOREIGN KEY (customer_id) REFERENCES customer(id);
CREATE INDEX idx_app_user_customer ON app_user (customer_id);
