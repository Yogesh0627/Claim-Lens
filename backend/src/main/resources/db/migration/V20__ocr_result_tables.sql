-- =====================================================
-- V20__ocr_result_tables.sql
-- Per-document OCR output produced by the OCR worker calling the Python OCR service. Worker-written
-- (plain tenant_id, no @TenantId) and append-oriented: one row per document OCR'd. Extracted signals
-- (registration / policy numbers) are stored comma-joined as best-effort hints for investigation.
-- =====================================================

CREATE TABLE ocr_result (
    id                   BIGSERIAL PRIMARY KEY,
    tenant_id            BIGINT NOT NULL,
    claim_id             BIGINT NOT NULL,
    document_id          BIGINT NOT NULL,

    engine               VARCHAR(30) NOT NULL,
    extracted_text       TEXT,
    confidence           NUMERIC(5,2),
    registration_numbers TEXT,
    policy_numbers       TEXT,

    created_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ocr_result_claim    FOREIGN KEY (claim_id) REFERENCES claim(id),
    CONSTRAINT fk_ocr_result_document FOREIGN KEY (document_id) REFERENCES document(id)
);
CREATE INDEX idx_ocr_result_claim ON ocr_result (tenant_id, claim_id);
