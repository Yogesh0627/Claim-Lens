-- Product documents: files attached to a product VERSION (e.g. the policy-wording PDF whose terms a
-- policy issued under that version is governed by). Distinct from `document` (claim files) — these
-- belong to no claim. Tenant-scoped like the rest of the product domain.
CREATE TABLE product_document (
    id                           BIGSERIAL PRIMARY KEY,
    tenant_id                    BIGINT NOT NULL,

    insurance_product_id         BIGINT NOT NULL,
    insurance_product_version_id BIGINT NOT NULL,

    document_type                VARCHAR(50) NOT NULL DEFAULT 'POLICY_WORDING',
    file_name                    VARCHAR(255) NOT NULL,
    content_type                 VARCHAR(100),
    size_bytes                   BIGINT,
    storage_key                  VARCHAR(512) NOT NULL,        -- pointer into object storage

    created_at                   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                   BIGINT,
    updated_at                   TIMESTAMPTZ,
    updated_by                   BIGINT,
    is_deleted                   BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at                   TIMESTAMPTZ,
    deleted_by                   BIGINT,

    CONSTRAINT fk_product_document_tenant  FOREIGN KEY (tenant_id) REFERENCES insurance_company(id),
    CONSTRAINT fk_product_document_product FOREIGN KEY (insurance_product_id) REFERENCES insurance_product(id),
    CONSTRAINT fk_product_document_version FOREIGN KEY (insurance_product_version_id) REFERENCES insurance_product_version(id)
);

CREATE INDEX idx_product_document_version
    ON product_document (insurance_product_version_id)
    WHERE is_deleted = FALSE;
