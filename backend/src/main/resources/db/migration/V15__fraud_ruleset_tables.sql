-- =====================================================
-- V15__fraud_ruleset_tables.sql
-- Config-driven fraud rules. NOTE the naming (decision D2): "ruleset" = admin-configurable business
-- rules; the word "policy" is reserved for the insurance contract. A fraud_ruleset is per
-- (tenant, claim_type); its fraud_rules toggle named checks and set their weights. The fraud engine
-- loads the ACTIVE ruleset and falls back to built-in defaults when none is configured.
-- =====================================================

CREATE TABLE fraud_ruleset (
    id               BIGSERIAL PRIMARY KEY,
    tenant_id        BIGINT NOT NULL,
    claim_type_id    BIGINT NOT NULL,

    name             VARCHAR(255) NOT NULL,
    medium_threshold INTEGER NOT NULL DEFAULT 25,
    high_threshold   INTEGER NOT NULL DEFAULT 50,
    status           VARCHAR(20) NOT NULL DEFAULT 'DRAFT',

    created_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by       BIGINT,
    updated_at       TIMESTAMPTZ,
    updated_by       BIGINT,
    is_deleted       BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at       TIMESTAMPTZ,
    deleted_by       BIGINT,

    CONSTRAINT fk_fraud_ruleset_tenant     FOREIGN KEY (tenant_id) REFERENCES insurance_company(id),
    CONSTRAINT fk_fraud_ruleset_claim_type FOREIGN KEY (claim_type_id) REFERENCES claim_type(id),
    CONSTRAINT chk_fraud_ruleset_status    CHECK (status IN ('DRAFT', 'ACTIVE', 'RETIRED')),
    CONSTRAINT chk_fraud_ruleset_thresholds CHECK (high_threshold >= medium_threshold)
);
-- At most one ACTIVE fraud ruleset per (tenant, claim_type).
CREATE UNIQUE INDEX uq_fraud_ruleset_active
    ON fraud_ruleset (tenant_id, claim_type_id) WHERE status = 'ACTIVE' AND is_deleted = FALSE;
CREATE INDEX idx_fraud_ruleset_tenant ON fraud_ruleset (tenant_id);

CREATE TABLE fraud_rule (
    id               BIGSERIAL PRIMARY KEY,
    tenant_id        BIGINT NOT NULL,
    fraud_ruleset_id BIGINT NOT NULL,

    code             VARCHAR(50) NOT NULL,   -- maps to a FraudRuleEvaluator, e.g. AMOUNT_OVER_SUM_INSURED
    description      TEXT,
    weight           INTEGER NOT NULL,
    enabled          BOOLEAN NOT NULL DEFAULT TRUE,

    created_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by       BIGINT,
    updated_at       TIMESTAMPTZ,
    updated_by       BIGINT,
    is_deleted       BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at       TIMESTAMPTZ,
    deleted_by       BIGINT,

    CONSTRAINT fk_fraud_rule_tenant  FOREIGN KEY (tenant_id) REFERENCES insurance_company(id),
    CONSTRAINT fk_fraud_rule_ruleset FOREIGN KEY (fraud_ruleset_id) REFERENCES fraud_ruleset(id),
    CONSTRAINT chk_fraud_rule_weight CHECK (weight >= 0)
);
CREATE UNIQUE INDEX uq_fraud_rule_code
    ON fraud_rule (fraud_ruleset_id, code) WHERE is_deleted = FALSE;
CREATE INDEX idx_fraud_rule_tenant ON fraud_rule (tenant_id);

-- Ruleset-configuration permissions.
INSERT INTO permission (code, name, module) VALUES
    ('RULESET_READ',  'Read rulesets',   'RULESET'),
    ('RULESET_WRITE', 'Manage rulesets', 'RULESET');

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.code = 'TENANT_ADMIN' AND p.module = 'RULESET';
