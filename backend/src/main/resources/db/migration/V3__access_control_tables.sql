-- =====================================================
-- V3__access_control_tables.sql
-- =====================================================

-- =====================================================
-- ROLE
-- =====================================================

CREATE TABLE role
(
    id BIGSERIAL PRIMARY KEY,


    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,

    is_system_role BOOLEAN NOT NULL DEFAULT TRUE,

    status VARCHAR(50) NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,

    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by BIGINT,

    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,

    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by BIGINT,

    CONSTRAINT uq_role_code UNIQUE (code),

    CONSTRAINT chk_role_status
        CHECK (status IN ('ACTIVE', 'INACTIVE'))


);

CREATE INDEX idx_role_status
    ON role(status);

CREATE INDEX idx_role_is_deleted
    ON role(is_deleted);

-- =====================================================
-- PERMISSION
-- =====================================================

CREATE TABLE permission
(
    id BIGSERIAL PRIMARY KEY,


    code VARCHAR(150) NOT NULL,
    name VARCHAR(255) NOT NULL,
    module VARCHAR(100) NOT NULL,
    description TEXT,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_permission_code UNIQUE (code)


);

CREATE INDEX idx_permission_module
    ON permission(module);

-- =====================================================
-- ROLE_PERMISSION
-- =====================================================

CREATE TABLE role_permission
(
    id BIGSERIAL PRIMARY KEY,


    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,

    CONSTRAINT fk_role_permission_role
        FOREIGN KEY (role_id)
            REFERENCES role(id),

    CONSTRAINT fk_role_permission_permission
        FOREIGN KEY (permission_id)
            REFERENCES permission(id),

    CONSTRAINT uq_role_permission
        UNIQUE (role_id, permission_id)


);

CREATE INDEX idx_role_permission_role_id
    ON role_permission(role_id);

CREATE INDEX idx_role_permission_permission_id
    ON role_permission(permission_id);

-- =====================================================
-- SYSTEM ROLE SEED DATA
-- =====================================================

INSERT INTO role
(
    code,
    name,
    description,
    is_system_role,
    status
)
VALUES
    (
        'TENANT_ADMIN',
        'Tenant Administrator',
        'Insurance company administrator with full tenant access.',
        TRUE,
        'ACTIVE'
    ),
    (
        'PRODUCT_MANAGER',
        'Product Manager',
        'Manages insurance products, product versions, coverages and exclusions.',
        TRUE,
        'ACTIVE'
    ),
    (
        'CLAIMS_MANAGER',
        'Claims Manager',
        'Manages claim processing operations and claim lifecycle.',
        TRUE,
        'ACTIVE'
    ),
    (
        'INVESTIGATION_MANAGER',
        'Investigation Manager',
        'Manages investigators and investigation assignments.',
        TRUE,
        'ACTIVE'
    ),
    (
        'INVESTIGATOR',
        'Investigator',
        'Performs claim investigations and submits investigation reports.',
        TRUE,
        'ACTIVE'
    ),
    (
        'CLAIMS_ADJUSTER',
        'Claims Adjuster',
        'Validates policy coverage, assesses damages and recommends settlements.',
        TRUE,
        'ACTIVE'
    ),
    (
        'CUSTOMER_SUPPORT',
        'Customer Support',
        'Handles customer communication and claim assistance.',
        TRUE,
        'ACTIVE'
    ),
    (
        'AUDITOR',
        'Auditor',
        'Performs audit and compliance reviews with read-only access.',
        TRUE,
        'ACTIVE'
    ),
    (
        'ANALYST',
        'Analyst',
        'Performs fraud analysis, reporting and operational analytics.',
        TRUE,
        'ACTIVE'
    );
