-- =====================================================
-- V25__document_version_tables.sql
-- Document versioning (Phase 6 completion). A `document` is now the LOGICAL document; each uploaded
-- revision is a `document_version` row. Old versions are never mutated — a re-upload appends a new
-- version and repoints `document.current_version_id`. The `document`'s own file_name/storage_key/etc.
-- stay as a mirror of the CURRENT version, so OCR and download keep working off `document` unchanged.
-- =====================================================

CREATE TABLE document_version (
    id             BIGSERIAL PRIMARY KEY,
    tenant_id      BIGINT NOT NULL,

    document_id    BIGINT NOT NULL,
    version_number INT NOT NULL,
    file_name      VARCHAR(255) NOT NULL,
    content_type   VARCHAR(100),
    size_bytes     BIGINT,
    storage_key    VARCHAR(512) NOT NULL,        -- pointer into object storage
    status         VARCHAR(20) NOT NULL DEFAULT 'UPLOADED',

    created_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by     BIGINT,
    updated_at     TIMESTAMPTZ,
    updated_by     BIGINT,
    is_deleted     BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at     TIMESTAMPTZ,
    deleted_by     BIGINT,

    CONSTRAINT fk_docver_tenant   FOREIGN KEY (tenant_id) REFERENCES insurance_company(id),
    CONSTRAINT fk_docver_document FOREIGN KEY (document_id) REFERENCES document(id),
    CONSTRAINT chk_docver_status  CHECK (status IN ('UPLOADED', 'INVALID', 'ARCHIVED'))
);

-- One row per (document, version_number) among live rows — partial so a soft-deleted version never
-- blocks reuse of its number.
CREATE UNIQUE INDEX uq_docver_number ON document_version (document_id, version_number)
    WHERE is_deleted = FALSE;
CREATE INDEX idx_docver_document ON document_version (document_id);
CREATE INDEX idx_docver_tenant   ON document_version (tenant_id);

-- The live version pointer. Nullable: legacy single-version documents predating this migration have
-- no version rows; the service backfills a v1 lazily / on next read. FK added after the table exists.
ALTER TABLE document ADD COLUMN current_version_id BIGINT;
ALTER TABLE document ADD CONSTRAINT fk_document_current_version
    FOREIGN KEY (current_version_id) REFERENCES document_version(id);
