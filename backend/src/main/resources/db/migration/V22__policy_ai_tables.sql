-- =====================================================
-- V22__policy_ai_tables.sql
-- Policy-intelligence (RAG): chunked, embedded policy-wording text per product VERSION, plus a log of
-- coverage Q&A with citations back to the chunks used.
--
-- Deliberate V1 simplifications (documented):
--   * NO pgvector. `CREATE EXTENSION vector` is not available on the local Postgres (and needs elevated
--     RDS privileges), so embeddings are stored as JSON text and cosine-ranked in the service. Fine at
--     V1 scale (hundreds of chunks per version); swap to pgvector + HNSW for ANN scaling later.
--   * Every table carries tenant_id and is mapped @TenantId (D7) — retrieval can never leak across
--     tenants via a forgotten join.
--   * Retrieval filters by insurance_product_version_id — the PINNED version, so answers reflect the
--     terms the customer actually contracted under.
-- =====================================================

CREATE TABLE policy_chunk (
    id                           BIGSERIAL PRIMARY KEY,
    tenant_id                    BIGINT NOT NULL,
    insurance_product_id         BIGINT NOT NULL,
    insurance_product_version_id BIGINT NOT NULL,

    chunk_index                  INT NOT NULL,
    chunk_text                   TEXT NOT NULL,
    embedding                    TEXT NOT NULL,          -- JSON array of floats
    embedding_model              VARCHAR(100) NOT NULL,

    created_at                   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_policy_chunk_version
        FOREIGN KEY (insurance_product_version_id) REFERENCES insurance_product_version(id)
);
CREATE INDEX idx_policy_chunk_version ON policy_chunk (tenant_id, insurance_product_version_id);

CREATE TABLE coverage_answer (
    id                           BIGSERIAL PRIMARY KEY,
    tenant_id                    BIGINT NOT NULL,
    insurance_product_version_id BIGINT NOT NULL,
    claim_id                     BIGINT,

    question                     TEXT NOT NULL,
    answer                       TEXT NOT NULL,
    model                        VARCHAR(100) NOT NULL,
    created_by                   BIGINT,

    created_at                   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_coverage_answer_version ON coverage_answer (tenant_id, insurance_product_version_id);

CREATE TABLE coverage_answer_citation (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT NOT NULL,
    coverage_answer_id  BIGINT NOT NULL,
    policy_chunk_id     BIGINT NOT NULL,
    citation_rank       INT NOT NULL,
    score               NUMERIC(7,6),

    CONSTRAINT fk_citation_answer FOREIGN KEY (coverage_answer_id) REFERENCES coverage_answer(id),
    CONSTRAINT fk_citation_chunk  FOREIGN KEY (policy_chunk_id) REFERENCES policy_chunk(id)
);

-- Permissions
INSERT INTO permission (code, name, module) VALUES
    ('COVERAGE_READ',  'Ask policy coverage questions', 'COVERAGE'),
    ('COVERAGE_WRITE', 'Ingest policy knowledge',       'COVERAGE');

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE p.code = 'COVERAGE_READ'
  AND r.code IN ('TENANT_ADMIN', 'PRODUCT_MANAGER', 'INVESTIGATION_MANAGER',
                 'INVESTIGATOR', 'CLAIMS_ADJUSTER', 'CLAIMS_MANAGER');

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE p.code = 'COVERAGE_WRITE'
  AND r.code IN ('TENANT_ADMIN', 'PRODUCT_MANAGER');
