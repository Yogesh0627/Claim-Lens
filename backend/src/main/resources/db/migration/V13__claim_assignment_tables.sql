-- =====================================================
-- V13__claim_assignment_tables.sql
-- Manual assignment + the CLAIM_ASSIGN / CLAIM_DECIDE permissions that complete the thin-slice
-- workflow (submit -> upload -> assign -> decide). In the manual slice OCR/analysis/fraud are
-- stubbed, so an assignment moves the claim straight to UNDER_INVESTIGATION.
-- =====================================================

CREATE TABLE claim_assignment (
    id                   BIGSERIAL PRIMARY KEY,
    tenant_id            BIGINT NOT NULL,
    claim_id             BIGINT NOT NULL,
    investigator_user_id BIGINT NOT NULL,
    assigned_by          BIGINT,
    status               VARCHAR(20) NOT NULL DEFAULT 'ASSIGNED',
    assigned_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_assignment_tenant       FOREIGN KEY (tenant_id) REFERENCES insurance_company(id),
    CONSTRAINT fk_assignment_claim        FOREIGN KEY (claim_id) REFERENCES claim(id),
    CONSTRAINT fk_assignment_investigator FOREIGN KEY (investigator_user_id) REFERENCES app_user(id),
    CONSTRAINT chk_assignment_status CHECK (status IN ('ASSIGNED', 'REASSIGNED', 'COMPLETED'))
);
CREATE INDEX idx_assignment_claim        ON claim_assignment (claim_id);
CREATE INDEX idx_assignment_investigator ON claim_assignment (tenant_id, investigator_user_id);

-- Workflow permissions
INSERT INTO permission (code, name, module) VALUES
    ('CLAIM_ASSIGN', 'Assign claims to investigators', 'CLAIM'),
    ('CLAIM_DECIDE', 'Approve or reject claims',        'CLAIM');

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.code = 'TENANT_ADMIN' AND p.code IN ('CLAIM_ASSIGN', 'CLAIM_DECIDE');

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.code = 'INVESTIGATION_MANAGER' AND p.code IN ('CLAIM_READ', 'CLAIM_ASSIGN');

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.code = 'INVESTIGATOR' AND p.code IN ('CLAIM_READ', 'CLAIM_DECIDE');
