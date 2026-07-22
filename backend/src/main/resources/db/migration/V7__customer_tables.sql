-- =====================================================
-- V7__customer_tables.sql
-- Customer = the policyholder (distinct from app_user, who are back-office staff).
-- Tenant-scoped. public_id UUID for opaque external URLs later.
-- =====================================================

CREATE TABLE customer (
    id               BIGSERIAL PRIMARY KEY,
    tenant_id        BIGINT NOT NULL,
    public_id        UUID NOT NULL DEFAULT gen_random_uuid(),

    customer_number  VARCHAR(50) NOT NULL,

    first_name       VARCHAR(100) NOT NULL,
    last_name        VARCHAR(100),

    email            VARCHAR(255),
    phone            VARCHAR(30),

    date_of_birth    DATE,
    national_id      VARCHAR(64),

    address          TEXT,
    city             VARCHAR(100),
    state            VARCHAR(100),
    country          VARCHAR(100),
    postal_code      VARCHAR(20),

    status           VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    created_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by       BIGINT,
    updated_at       TIMESTAMPTZ,
    updated_by       BIGINT,

    is_deleted       BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at       TIMESTAMPTZ,
    deleted_by       BIGINT,

    CONSTRAINT fk_customer_tenant
        FOREIGN KEY (tenant_id) REFERENCES insurance_company(id),
    CONSTRAINT chk_customer_status
        CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE UNIQUE INDEX uq_customer_tenant_number
    ON customer (tenant_id, customer_number) WHERE is_deleted = FALSE;
CREATE UNIQUE INDEX uq_customer_public_id
    ON customer (public_id);
CREATE INDEX idx_customer_tenant ON customer (tenant_id);
CREATE INDEX idx_customer_email ON customer (tenant_id, email);
CREATE INDEX idx_customer_national_id ON customer (tenant_id, national_id);

-- Global permissions for the customer module + mappings (roles referenced by code).
INSERT INTO permission (code, name, module) VALUES
    ('CUSTOMER_READ',  'Read customers',   'CUSTOMER'),
    ('CUSTOMER_WRITE', 'Manage customers', 'CUSTOMER');

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.code = 'TENANT_ADMIN' AND p.module = 'CUSTOMER';

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.code = 'CUSTOMER_SUPPORT' AND p.code = 'CUSTOMER_READ';
