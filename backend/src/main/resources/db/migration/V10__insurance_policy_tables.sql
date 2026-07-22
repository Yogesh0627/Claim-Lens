-- =====================================================
-- V10__insurance_policy_tables.sql
-- InsurancePolicy = the actual contract a customer bought. It is PINNED to a specific product
-- VERSION at sale (insurance_product_version_id) — a later claim inherits THAT version, never the
-- product's current one, so the claim is judged against the terms the customer agreed to.
-- =====================================================

CREATE TABLE insurance_policy (
    id                            BIGSERIAL PRIMARY KEY,
    tenant_id                     BIGINT NOT NULL,
    public_id                     UUID NOT NULL DEFAULT gen_random_uuid(),

    policy_number                 VARCHAR(100) NOT NULL,

    customer_id                   BIGINT NOT NULL,
    insurance_product_id          BIGINT NOT NULL,
    insurance_product_version_id  BIGINT NOT NULL,      -- pinned at sale

    issued_at                     TIMESTAMPTZ,
    effective_from                DATE NOT NULL,
    effective_to                  DATE NOT NULL,        -- a contract always has an end date

    sum_insured                   NUMERIC(18,2) NOT NULL,
    deductible                    NUMERIC(18,2) NOT NULL DEFAULT 0,
    premium_amount                NUMERIC(18,2),
    currency                      VARCHAR(3) NOT NULL DEFAULT 'INR',

    status                        VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    cancelled_at                  TIMESTAMPTZ,
    cancellation_reason           TEXT,

    issuing_branch_id             BIGINT,

    created_at                    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                    BIGINT,
    updated_at                    TIMESTAMPTZ,
    updated_by                    BIGINT,
    is_deleted                    BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at                    TIMESTAMPTZ,
    deleted_by                    BIGINT,

    CONSTRAINT fk_policy_tenant          FOREIGN KEY (tenant_id) REFERENCES insurance_company(id),
    CONSTRAINT fk_policy_customer        FOREIGN KEY (customer_id) REFERENCES customer(id),
    CONSTRAINT fk_policy_product         FOREIGN KEY (insurance_product_id) REFERENCES insurance_product(id),
    CONSTRAINT fk_policy_product_version FOREIGN KEY (insurance_product_version_id) REFERENCES insurance_product_version(id),
    CONSTRAINT fk_policy_branch          FOREIGN KEY (issuing_branch_id) REFERENCES branch(id),

    CONSTRAINT chk_policy_status      CHECK (status IN ('DRAFT', 'ACTIVE', 'LAPSED', 'CANCELLED', 'EXPIRED')),
    CONSTRAINT chk_policy_validity    CHECK (effective_to >= effective_from),
    CONSTRAINT chk_policy_sum_insured CHECK (sum_insured > 0)
);

CREATE UNIQUE INDEX uq_policy_tenant_number
    ON insurance_policy (tenant_id, policy_number) WHERE is_deleted = FALSE;
CREATE UNIQUE INDEX uq_policy_public_id ON insurance_policy (public_id);
CREATE INDEX idx_policy_tenant   ON insurance_policy (tenant_id);
CREATE INDEX idx_policy_customer ON insurance_policy (customer_id);
-- Drives the "policy active on loss date" lookup at claim intake.
CREATE INDEX idx_policy_coverage ON insurance_policy (tenant_id, policy_number, effective_from, effective_to);

CREATE TABLE insured_vehicle (
    id                             BIGSERIAL PRIMARY KEY,
    tenant_id                      BIGINT NOT NULL,
    insurance_policy_id            BIGINT NOT NULL,

    registration_number            VARCHAR(32) NOT NULL,
    registration_number_normalized VARCHAR(32) NOT NULL,  -- upper, no spaces/hyphens

    make                           VARCHAR(100) NOT NULL,
    model                          VARCHAR(100) NOT NULL,
    variant                        VARCHAR(100),
    manufacture_year               INTEGER,

    chassis_number                 VARCHAR(64),
    engine_number                  VARCHAR(64),

    colour                         VARCHAR(50),
    fuel_type                      VARCHAR(30),
    seating_capacity               INTEGER,
    idv                            NUMERIC(18,2),

    status                         VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

    created_at                     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                     BIGINT,
    updated_at                     TIMESTAMPTZ,
    updated_by                     BIGINT,
    is_deleted                     BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at                     TIMESTAMPTZ,
    deleted_by                     BIGINT,

    CONSTRAINT fk_vehicle_tenant FOREIGN KEY (tenant_id) REFERENCES insurance_company(id),
    CONSTRAINT fk_vehicle_policy FOREIGN KEY (insurance_policy_id) REFERENCES insurance_policy(id),
    CONSTRAINT chk_vehicle_status CHECK (status IN ('ACTIVE', 'REMOVED')),
    CONSTRAINT chk_vehicle_year   CHECK (manufacture_year IS NULL OR manufacture_year BETWEEN 1900 AND 2100)
);

-- V1 rule: exactly one active vehicle per motor policy (drop for fleet policies in V2).
CREATE UNIQUE INDEX uq_vehicle_one_active_per_policy
    ON insured_vehicle (insurance_policy_id) WHERE status = 'ACTIVE' AND is_deleted = FALSE;
CREATE INDEX idx_vehicle_tenant  ON insured_vehicle (tenant_id);
CREATE INDEX idx_vehicle_policy  ON insured_vehicle (insurance_policy_id);
CREATE INDEX idx_vehicle_reg     ON insured_vehicle (tenant_id, registration_number_normalized);
CREATE INDEX idx_vehicle_chassis ON insured_vehicle (tenant_id, chassis_number) WHERE chassis_number IS NOT NULL;

-- Permissions
INSERT INTO permission (code, name, module) VALUES
    ('POLICY_READ',  'Read insurance policies',   'POLICY'),
    ('POLICY_WRITE', 'Manage insurance policies', 'POLICY');

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.code = 'TENANT_ADMIN' AND p.module = 'POLICY';

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.code = 'CLAIMS_ADJUSTER' AND p.code = 'POLICY_READ';
