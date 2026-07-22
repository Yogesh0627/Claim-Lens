-- =====================================================
-- V11__claim_tables.sql
-- The claim: a request for payment against a policy after an incident. It stores the pinned
-- product version (inherited from the policy) plus write-once snapshots (policy_number,
-- vehicle_registration_number) that stay stable even if the source records are later corrected.
-- =====================================================

CREATE TABLE claim (
    id                            BIGSERIAL PRIMARY KEY,
    tenant_id                     BIGINT NOT NULL,
    public_id                     UUID NOT NULL DEFAULT gen_random_uuid(),

    claim_number                  VARCHAR(50) NOT NULL,

    customer_id                   BIGINT NOT NULL,
    insurance_policy_id           BIGINT NOT NULL,
    insured_vehicle_id            BIGINT,
    insurance_product_id          BIGINT NOT NULL,
    insurance_product_version_id  BIGINT NOT NULL,     -- inherited from the policy (version pinning)
    claim_type_id                 BIGINT NOT NULL,

    -- write-once snapshots
    policy_number                 VARCHAR(100) NOT NULL,
    vehicle_registration_number   VARCHAR(32),

    incident_date                 DATE NOT NULL,
    reported_date                 DATE,
    claim_amount                  NUMERIC(18,2),
    description                   TEXT,

    status                        VARCHAR(30) NOT NULL DEFAULT 'DRAFT',

    submitted_at                  TIMESTAMPTZ,
    approved_at                   TIMESTAMPTZ,
    rejected_at                   TIMESTAMPTZ,
    closed_at                     TIMESTAMPTZ,
    reopened_at                   TIMESTAMPTZ,

    created_at                    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                    BIGINT,
    updated_at                    TIMESTAMPTZ,
    updated_by                    BIGINT,
    is_deleted                    BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at                    TIMESTAMPTZ,
    deleted_by                    BIGINT,

    CONSTRAINT fk_claim_tenant          FOREIGN KEY (tenant_id) REFERENCES insurance_company(id),
    CONSTRAINT fk_claim_customer        FOREIGN KEY (customer_id) REFERENCES customer(id),
    CONSTRAINT fk_claim_policy          FOREIGN KEY (insurance_policy_id) REFERENCES insurance_policy(id),
    CONSTRAINT fk_claim_vehicle         FOREIGN KEY (insured_vehicle_id) REFERENCES insured_vehicle(id),
    CONSTRAINT fk_claim_product         FOREIGN KEY (insurance_product_id) REFERENCES insurance_product(id),
    CONSTRAINT fk_claim_product_version FOREIGN KEY (insurance_product_version_id) REFERENCES insurance_product_version(id),
    CONSTRAINT fk_claim_claim_type      FOREIGN KEY (claim_type_id) REFERENCES claim_type(id),

    CONSTRAINT chk_claim_status CHECK (status IN (
        'DRAFT', 'SUBMITTED', 'AWAITING_ANALYSIS', 'AWAITING_ASSIGNMENT', 'AWAITING_ACCEPTANCE',
        'UNDER_INVESTIGATION', 'WAITING_FOR_CUSTOMER', 'APPROVED', 'REJECTED', 'CLOSED', 'REOPENED'))
);

CREATE UNIQUE INDEX uq_claim_tenant_number ON claim (tenant_id, claim_number) WHERE is_deleted = FALSE;
CREATE UNIQUE INDEX uq_claim_public_id ON claim (public_id);
CREATE INDEX idx_claim_tenant   ON claim (tenant_id);
CREATE INDEX idx_claim_policy    ON claim (insurance_policy_id);
CREATE INDEX idx_claim_customer  ON claim (customer_id);
CREATE INDEX idx_claim_status    ON claim (tenant_id, status);

-- Append-only status history (who moved the claim from where to where, and why).
CREATE TABLE claim_status_history (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     BIGINT NOT NULL,
    claim_id      BIGINT NOT NULL,
    from_status   VARCHAR(30),
    to_status     VARCHAR(30) NOT NULL,
    note          TEXT,
    changed_by    BIGINT,
    changed_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_claim_history_tenant FOREIGN KEY (tenant_id) REFERENCES insurance_company(id),
    CONSTRAINT fk_claim_history_claim  FOREIGN KEY (claim_id) REFERENCES claim(id)
);
CREATE INDEX idx_claim_history_claim ON claim_status_history (claim_id);

-- Permissions
INSERT INTO permission (code, name, module) VALUES
    ('CLAIM_READ',   'Read claims',       'CLAIM'),
    ('CLAIM_WRITE',  'Create/edit claims','CLAIM'),
    ('CLAIM_SUBMIT', 'Submit claims',     'CLAIM');

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.code = 'TENANT_ADMIN' AND p.module = 'CLAIM';

-- Employees create & submit claims on behalf of customers; support can read.
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.code = 'CUSTOMER_SUPPORT' AND p.code IN ('CLAIM_READ', 'CLAIM_WRITE', 'CLAIM_SUBMIT');
