-- =====================================================
-- V12__document_tables.sql
-- Claim documents. The DB stores metadata + a storage_key (pointer); the bytes live in object
-- storage (S3/R2 in prod; local filesystem in dev). Document versioning is deferred to full Phase 6 —
-- this thin-slice table is single-version.
-- =====================================================

CREATE TABLE document (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     BIGINT NOT NULL,
    public_id     UUID NOT NULL DEFAULT gen_random_uuid(),

    claim_id      BIGINT,                       -- nullable: product docs (later) belong to no claim
    document_type VARCHAR(50) NOT NULL,
    file_name     VARCHAR(255) NOT NULL,
    content_type  VARCHAR(100),
    size_bytes    BIGINT,
    storage_key   VARCHAR(512) NOT NULL,        -- pointer into object storage
    status        VARCHAR(20) NOT NULL DEFAULT 'UPLOADED',

    uploaded_by   BIGINT,

    created_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    BIGINT,
    updated_at    TIMESTAMPTZ,
    updated_by    BIGINT,
    is_deleted    BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at    TIMESTAMPTZ,
    deleted_by    BIGINT,

    CONSTRAINT fk_document_tenant FOREIGN KEY (tenant_id) REFERENCES insurance_company(id),
    CONSTRAINT fk_document_claim  FOREIGN KEY (claim_id) REFERENCES claim(id),
    CONSTRAINT chk_document_status CHECK (status IN ('UPLOADED', 'INVALID', 'ARCHIVED'))
);

CREATE UNIQUE INDEX uq_document_public_id ON document (public_id);
CREATE INDEX idx_document_tenant ON document (tenant_id);
CREATE INDEX idx_document_claim  ON document (claim_id);
