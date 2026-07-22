-- =====================================================
-- V8__reference_data_tables.sql
-- claim_type is a GLOBAL reference table (shared across tenants, like role) — V1 ships MOTOR only.
-- insurance_product.claim_type_id references it, so it must precede the product tables.
-- =====================================================

CREATE TABLE claim_type (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(50) NOT NULL,
    name        VARCHAR(150) NOT NULL,
    description TEXT,
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_claim_type_code UNIQUE (code),
    CONSTRAINT chk_claim_type_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

INSERT INTO claim_type (code, name, description) VALUES
    ('MOTOR', 'Motor Insurance Claim', 'Claims for motor/vehicle insurance policies.');
