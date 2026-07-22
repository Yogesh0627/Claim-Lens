-- =====================================================
-- V17__audit_tables.sql
-- Audit trail. Append-only by design (no updated_at, no is_deleted) — an audit log you can edit or
-- delete is not an audit log. Written automatically by the AuditAspect for @Auditable actions.
-- =====================================================

CREATE TABLE audit_log (
    id           BIGSERIAL PRIMARY KEY,
    tenant_id    BIGINT NOT NULL,
    user_id      BIGINT,

    action       VARCHAR(50) NOT NULL,
    entity_type  VARCHAR(50) NOT NULL,
    entity_id    BIGINT,
    details      TEXT,

    created_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_audit_log_tenant FOREIGN KEY (tenant_id) REFERENCES insurance_company(id)
);
CREATE INDEX idx_audit_log_entity ON audit_log (entity_type, entity_id);
CREATE INDEX idx_audit_log_tenant ON audit_log (tenant_id, created_at);

INSERT INTO permission (code, name, module) VALUES
    ('AUDIT_READ', 'Read the audit trail', 'AUDIT');

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.code IN ('TENANT_ADMIN', 'AUDITOR', 'INVESTIGATION_MANAGER') AND p.code = 'AUDIT_READ';
