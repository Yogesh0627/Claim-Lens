-- =====================================================
-- V16__investigation_tables.sql
-- Investigation notes: what an investigator records while working a claim (site visits, fraud
-- observations, customer interactions, manager reviews, escalations). Per design decision,
-- investigation_note supersedes the Finding/Evidence split — it carries an optional severity and an
-- optional document_id (a note *about* a photo points at the photo).
-- =====================================================

CREATE TABLE investigation_note (
    id           BIGSERIAL PRIMARY KEY,
    tenant_id    BIGINT NOT NULL,
    claim_id     BIGINT NOT NULL,

    note_type    VARCHAR(30) NOT NULL,
    note         TEXT NOT NULL,
    severity     VARCHAR(10),
    document_id  BIGINT,

    created_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by   BIGINT,
    updated_at   TIMESTAMPTZ,
    updated_by   BIGINT,
    is_deleted   BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at   TIMESTAMPTZ,
    deleted_by   BIGINT,

    CONSTRAINT fk_investigation_note_tenant   FOREIGN KEY (tenant_id) REFERENCES insurance_company(id),
    CONSTRAINT fk_investigation_note_claim    FOREIGN KEY (claim_id) REFERENCES claim(id),
    CONSTRAINT fk_investigation_note_document FOREIGN KEY (document_id) REFERENCES document(id),
    CONSTRAINT chk_investigation_note_type CHECK (note_type IN (
        'FRAUD_OBSERVATION', 'SITE_VISIT', 'CUSTOMER_INTERACTION', 'MANAGER_REVIEW',
        'ESCALATION', 'GENERAL')),
    CONSTRAINT chk_investigation_note_severity CHECK (severity IS NULL OR severity IN ('LOW','MEDIUM','HIGH'))
);
CREATE INDEX idx_investigation_note_claim  ON investigation_note (claim_id);
CREATE INDEX idx_investigation_note_tenant ON investigation_note (tenant_id);

-- Investigation permission.
INSERT INTO permission (code, name, module) VALUES
    ('CLAIM_INVESTIGATE', 'Record investigation notes', 'CLAIM');

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.code IN ('TENANT_ADMIN', 'INVESTIGATOR', 'INVESTIGATION_MANAGER')
  AND p.code = 'CLAIM_INVESTIGATE';
