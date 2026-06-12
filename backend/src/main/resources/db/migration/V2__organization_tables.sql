-- ============================================================================
-- ORGANIZATION DOMAIN
-- ============================================================================
-- Tables:
--   1. insurance_company
--   2. region
--   3. branch
--
-- Notes:
--   - Multi-tenant root = insurance_company
--   - Soft delete enabled
--   - Audit columns enabled
--   - No user foreign keys yet
--   - No ON DELETE CASCADE
-- ============================================================================


-- ============================================================================
-- 1. INSURANCE COMPANY
-- ============================================================================

CREATE TABLE insurance_company (
                                   id BIGSERIAL PRIMARY KEY,

                                   name VARCHAR(255) NOT NULL,
                                   code VARCHAR(50) NOT NULL,
                                   tenant_key VARCHAR(100) NOT NULL,

                                   status VARCHAR(50) NOT NULL DEFAULT 'ONBOARDING',

                                   contact_email VARCHAR(255),
                                   contact_phone VARCHAR(50),

                                   website VARCHAR(255),

                                   head_office_address TEXT,

                                   logo_url TEXT,

                                   branding_config JSONB,

                                   subscription_plan VARCHAR(50) NOT NULL DEFAULT 'BASIC',

                                   currency VARCHAR(10) NOT NULL DEFAULT 'INR',
                                   timezone VARCHAR(100) NOT NULL DEFAULT 'Asia/Kolkata',

                                   created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   created_by BIGINT,

                                   updated_at TIMESTAMP WITH TIME ZONE,
                                   updated_by BIGINT,

                                   is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
                                   deleted_at TIMESTAMP WITH TIME ZONE,
                                   deleted_by BIGINT,

                                   CONSTRAINT chk_insurance_company_status
                                       CHECK (
                                           status IN (
                                                      'ACTIVE',
                                                      'ONBOARDING',
                                                      'SUSPENDED'
                                               )
                                           ),

                                   CONSTRAINT chk_insurance_company_subscription_plan
                                       CHECK (
                                           subscription_plan IN (
                                                                 'BASIC',
                                                                 'ENTERPRISE',
                                                                 'CUSTOM'
                                               )
                                           )
);

CREATE UNIQUE INDEX uq_insurance_company_code
    ON insurance_company(code)
    WHERE is_deleted = FALSE;

CREATE UNIQUE INDEX uq_insurance_company_tenant_key
    ON insurance_company(tenant_key)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_insurance_company_status
    ON insurance_company(status);



-- ============================================================================
-- 2. REGION
-- ============================================================================

CREATE TABLE region (
                        id BIGSERIAL PRIMARY KEY,

                        tenant_id BIGINT NOT NULL,

                        code VARCHAR(50) NOT NULL,
                        name VARCHAR(255) NOT NULL,

                        owner_user_id BIGINT,

                        status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',

                        description TEXT,

                        created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        created_by BIGINT,

                        updated_at TIMESTAMP WITH TIME ZONE,
                        updated_by BIGINT,

                        is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
                        deleted_at TIMESTAMP WITH TIME ZONE,
                        deleted_by BIGINT,

                        CONSTRAINT fk_region_tenant
                            FOREIGN KEY (tenant_id)
                                REFERENCES insurance_company(id),

                        CONSTRAINT chk_region_status
                            CHECK (
                                status IN (
                                           'ACTIVE',
                                           'INACTIVE'
                                    )
                                )
);

CREATE UNIQUE INDEX uq_region_tenant_code
    ON region(tenant_id, code)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_region_tenant_lookup
    ON region(tenant_id);

CREATE INDEX idx_region_status
    ON region(status);

CREATE INDEX idx_region_owner
    ON region(owner_user_id);



-- ============================================================================
-- 3. BRANCH
-- ============================================================================

CREATE TABLE branch (
                        id BIGSERIAL PRIMARY KEY,

                        tenant_id BIGINT NOT NULL,

                        region_id BIGINT NOT NULL,

                        code VARCHAR(50) NOT NULL,
                        name VARCHAR(255) NOT NULL,

                        owner_user_id BIGINT,

                        email VARCHAR(255),
                        phone VARCHAR(50),

                        address TEXT,

                        city VARCHAR(100),
                        state VARCHAR(100),
                        country VARCHAR(100),

                        postal_code VARCHAR(20),

                        status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',

                        description TEXT,

                        created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        created_by BIGINT,

                        updated_at TIMESTAMP WITH TIME ZONE,
                        updated_by BIGINT,

                        is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
                        deleted_at TIMESTAMP WITH TIME ZONE,
                        deleted_by BIGINT,

                        CONSTRAINT fk_branch_tenant
                            FOREIGN KEY (tenant_id)
                                REFERENCES insurance_company(id),

                        CONSTRAINT fk_branch_region
                            FOREIGN KEY (region_id)
                                REFERENCES region(id),

                        CONSTRAINT chk_branch_status
                            CHECK (
                                status IN (
                                           'ACTIVE',
                                           'INACTIVE'
                                    )
                                )
);

CREATE UNIQUE INDEX uq_branch_tenant_code
    ON branch(tenant_id, code)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_branch_tenant_lookup
    ON branch(tenant_id);

CREATE INDEX idx_branch_region_lookup
    ON branch(region_id);

CREATE INDEX idx_branch_status
    ON branch(status);

CREATE INDEX idx_branch_owner
    ON branch(owner_user_id);