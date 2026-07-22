-- =====================================================
-- V21__analysis_result_tables.sql
-- Per-image analysis signals from the analysis worker calling the Python analysis service. Worker-
-- written (plain tenant_id). Perceptual hashes enable cross-claim duplicate detection; exif_state and
-- the synthetic signal are soft hints. duplicate_of_claim_id is set when the same pHash already exists
-- on another claim (photo reuse — the top motor-fraud pattern).
-- =====================================================

CREATE TABLE analysis_result (
    id                    BIGSERIAL PRIMARY KEY,
    tenant_id             BIGINT NOT NULL,
    claim_id              BIGINT NOT NULL,
    document_id           BIGINT NOT NULL,

    phash                 VARCHAR(64),
    dhash                 VARCHAR(64),
    average_hash          VARCHAR(64),
    exif_state            VARCHAR(20),
    synthetic_signal      BOOLEAN NOT NULL DEFAULT FALSE,
    synthetic_score       NUMERIC(5,4),
    duplicate_of_claim_id BIGINT,

    created_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_analysis_result_claim    FOREIGN KEY (claim_id) REFERENCES claim(id),
    CONSTRAINT fk_analysis_result_document FOREIGN KEY (document_id) REFERENCES document(id)
);
CREATE INDEX idx_analysis_result_claim ON analysis_result (tenant_id, claim_id);
CREATE INDEX idx_analysis_result_phash ON analysis_result (tenant_id, phash);
