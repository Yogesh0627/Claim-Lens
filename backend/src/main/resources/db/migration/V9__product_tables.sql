-- =====================================================
-- V9__product_tables.sql
-- InsuranceProduct = what an insurer sells (e.g. PRIVATE_CAR_PREMIUM), versioned over time.
-- A policy is later pinned to a specific product VERSION (the terms in force at sale).
-- =====================================================

CREATE TABLE insurance_product (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     BIGINT NOT NULL,
    claim_type_id BIGINT NOT NULL,

    code          VARCHAR(50) NOT NULL,
    name          VARCHAR(255) NOT NULL,
    description   TEXT,

    status        VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    created_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    BIGINT,
    updated_at    TIMESTAMPTZ,
    updated_by    BIGINT,
    is_deleted    BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at    TIMESTAMPTZ,
    deleted_by    BIGINT,

    CONSTRAINT fk_product_tenant     FOREIGN KEY (tenant_id) REFERENCES insurance_company(id),
    CONSTRAINT fk_product_claim_type FOREIGN KEY (claim_type_id) REFERENCES claim_type(id),
    CONSTRAINT chk_product_status    CHECK (status IN ('ACTIVE', 'INACTIVE', 'RETIRED'))
);

CREATE UNIQUE INDEX uq_product_tenant_code
    ON insurance_product (tenant_id, code) WHERE is_deleted = FALSE;
CREATE INDEX idx_product_tenant ON insurance_product (tenant_id);

CREATE TABLE insurance_product_version (
    id                    BIGSERIAL PRIMARY KEY,
    tenant_id             BIGINT NOT NULL,
    insurance_product_id  BIGINT NOT NULL,

    version_number        INTEGER NOT NULL,
    status                VARCHAR(20) NOT NULL DEFAULT 'DRAFT',

    effective_from        DATE NOT NULL,
    effective_to          DATE,                 -- open-ended until superseded

    coverage_summary      TEXT,

    created_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by            BIGINT,
    updated_at            TIMESTAMPTZ,
    updated_by            BIGINT,
    is_deleted            BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at            TIMESTAMPTZ,
    deleted_by            BIGINT,

    CONSTRAINT fk_product_version_tenant  FOREIGN KEY (tenant_id) REFERENCES insurance_company(id),
    CONSTRAINT fk_product_version_product FOREIGN KEY (insurance_product_id) REFERENCES insurance_product(id),
    CONSTRAINT chk_product_version_status CHECK (status IN ('DRAFT', 'ACTIVE', 'RETIRED')),
    CONSTRAINT chk_product_version_dates  CHECK (effective_to IS NULL OR effective_to >= effective_from)
);

CREATE UNIQUE INDEX uq_product_version_number
    ON insurance_product_version (insurance_product_id, version_number) WHERE is_deleted = FALSE;
-- At most one ACTIVE version per product.
CREATE UNIQUE INDEX uq_product_version_active
    ON insurance_product_version (insurance_product_id)
    WHERE status = 'ACTIVE' AND is_deleted = FALSE;
CREATE INDEX idx_product_version_tenant ON insurance_product_version (tenant_id);

-- Permissions
INSERT INTO permission (code, name, module) VALUES
    ('PRODUCT_READ',  'Read products',   'PRODUCT'),
    ('PRODUCT_WRITE', 'Manage products', 'PRODUCT');

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.code = 'TENANT_ADMIN' AND p.module = 'PRODUCT';

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.code = 'PRODUCT_MANAGER' AND p.module = 'PRODUCT';
