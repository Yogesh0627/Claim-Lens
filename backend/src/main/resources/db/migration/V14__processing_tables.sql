-- =====================================================
-- V14__processing_tables.sql
-- The async processing pipeline. These tables are NOT tenant-filtered by Hibernate @TenantId: the
-- background workers run outside any request, so they must query jobs across all tenants. Each row
-- carries a plain tenant_id that a worker restores into TenantContext while processing.
--
-- claim_processing_state has UNIQUE(claim_id): one row per claim = one lock target, which is what
-- makes the "exactly one fraud job per claim" gate work (a conditional atomic UPDATE on this row).
-- =====================================================

CREATE TABLE claim_processing_state (
    id                 BIGSERIAL PRIMARY KEY,
    tenant_id          BIGINT NOT NULL,
    claim_id           BIGINT NOT NULL,

    ocr_status         VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED',
    analysis_status    VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED',
    fraud_status       VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED',

    pending_reprocess  BOOLEAN NOT NULL DEFAULT FALSE,
    fraud_job_id       BIGINT,

    last_updated_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_cps_tenant FOREIGN KEY (tenant_id) REFERENCES insurance_company(id),
    CONSTRAINT fk_cps_claim  FOREIGN KEY (claim_id) REFERENCES claim(id)
);
CREATE UNIQUE INDEX uq_cps_claim ON claim_processing_state (claim_id);

-- Generic job shape reused for ocr_job / analysis_job / fraud_job.
CREATE TABLE ocr_job (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     BIGINT NOT NULL,
    claim_id      BIGINT NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_count INTEGER NOT NULL DEFAULT 0,
    locked_by     VARCHAR(255),
    locked_at     TIMESTAMPTZ,
    failure_reason TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ,
    CONSTRAINT fk_ocr_job_tenant FOREIGN KEY (tenant_id) REFERENCES insurance_company(id),
    CONSTRAINT fk_ocr_job_claim  FOREIGN KEY (claim_id) REFERENCES claim(id),
    CONSTRAINT chk_ocr_job_status CHECK (status IN ('PENDING','PROCESSING','COMPLETE','FAILED','DEAD_LETTER'))
);
CREATE INDEX idx_ocr_job_pending ON ocr_job (created_at) WHERE status = 'PENDING';

CREATE TABLE analysis_job (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     BIGINT NOT NULL,
    claim_id      BIGINT NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_count INTEGER NOT NULL DEFAULT 0,
    locked_by     VARCHAR(255),
    locked_at     TIMESTAMPTZ,
    failure_reason TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ,
    CONSTRAINT fk_analysis_job_tenant FOREIGN KEY (tenant_id) REFERENCES insurance_company(id),
    CONSTRAINT fk_analysis_job_claim  FOREIGN KEY (claim_id) REFERENCES claim(id),
    CONSTRAINT chk_analysis_job_status CHECK (status IN ('PENDING','PROCESSING','COMPLETE','FAILED','DEAD_LETTER'))
);
CREATE INDEX idx_analysis_job_pending ON analysis_job (created_at) WHERE status = 'PENDING';

CREATE TABLE fraud_job (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     BIGINT NOT NULL,
    claim_id      BIGINT NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_count INTEGER NOT NULL DEFAULT 0,
    locked_by     VARCHAR(255),
    locked_at     TIMESTAMPTZ,
    failure_reason TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ,
    CONSTRAINT fk_fraud_job_tenant FOREIGN KEY (tenant_id) REFERENCES insurance_company(id),
    CONSTRAINT fk_fraud_job_claim  FOREIGN KEY (claim_id) REFERENCES claim(id),
    CONSTRAINT chk_fraud_job_status CHECK (status IN ('PENDING','PROCESSING','COMPLETE','FAILED','DEAD_LETTER'))
);
-- Layer 2 defence: the DB itself makes "two live fraud jobs for one claim" unrepresentable.
CREATE UNIQUE INDEX uq_fraud_job_one_live_per_claim
    ON fraud_job (claim_id) WHERE status IN ('PENDING','PROCESSING');
CREATE INDEX idx_fraud_job_pending ON fraud_job (created_at) WHERE status = 'PENDING';

CREATE TABLE fraud_score (
    id           BIGSERIAL PRIMARY KEY,
    tenant_id    BIGINT NOT NULL,
    claim_id     BIGINT NOT NULL,
    score        INTEGER NOT NULL,
    risk_level   VARCHAR(10) NOT NULL,
    explanation  TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_fraud_score_tenant FOREIGN KEY (tenant_id) REFERENCES insurance_company(id),
    CONSTRAINT fk_fraud_score_claim  FOREIGN KEY (claim_id) REFERENCES claim(id),
    CONSTRAINT chk_fraud_score_risk CHECK (risk_level IN ('LOW','MEDIUM','HIGH'))
);
CREATE INDEX idx_fraud_score_claim ON fraud_score (claim_id);

-- Fraud read permission (the engine writes scores; managers/investigators read them).
INSERT INTO permission (code, name, module) VALUES
    ('FRAUD_READ', 'Read fraud scores', 'FRAUD');
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.code IN ('TENANT_ADMIN','INVESTIGATION_MANAGER','INVESTIGATOR') AND p.code = 'FRAUD_READ';
