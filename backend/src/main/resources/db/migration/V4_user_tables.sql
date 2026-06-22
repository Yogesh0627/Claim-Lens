-- =====================================================
-- Department
-- =====================================================

CREATE TABLE department (
                            id BIGSERIAL PRIMARY KEY,

                            tenant_id BIGINT NOT NULL,

                            code VARCHAR(50) NOT NULL,
                            name VARCHAR(150) NOT NULL,
                            description TEXT,

                            status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            created_by BIGINT,
                            updated_at TIMESTAMP,
                            updated_by BIGINT,

                            is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
                            deleted_at TIMESTAMP,
                            deleted_by BIGINT,

                            CONSTRAINT fk_department_tenant
                                FOREIGN KEY (tenant_id)
                                    REFERENCES insurance_company(id),

                            CONSTRAINT chk_department_status
                                    CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE UNIQUE INDEX uq_department_tenant_code
    ON department (tenant_id, code)
    WHERE is_deleted = FALSE;

-- =====================================================
-- Designation
-- =====================================================

CREATE TABLE designation (
                             id BIGSERIAL PRIMARY KEY,

                             tenant_id BIGINT NOT NULL,

                             code VARCHAR(50) NOT NULL,
                             name VARCHAR(150) NOT NULL,
                             description TEXT,

                             status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

                             created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             created_by BIGINT,
                             updated_at TIMESTAMP,
                             updated_by BIGINT,

                             is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
                             deleted_at TIMESTAMP,
                             deleted_by BIGINT,

                             CONSTRAINT fk_designation_tenant
                                 FOREIGN KEY (tenant_id)
                                     REFERENCES insurance_company(id),

                             CONSTRAINT chk_designation_status
                                     CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE UNIQUE INDEX uq_designation_tenant_code
    ON designation (tenant_id, code)
    WHERE is_deleted = FALSE;

-- =====================================================
-- App User
-- =====================================================

CREATE TABLE app_user (
                          id BIGSERIAL PRIMARY KEY,

                          tenant_id BIGINT NOT NULL,

                          employee_code VARCHAR(50) NOT NULL,

                          first_name VARCHAR(100) NOT NULL,
                          last_name VARCHAR(100),

                          email VARCHAR(255) NOT NULL,
                          phone VARCHAR(30),

                          password_hash VARCHAR(500),

                          role_id BIGINT NOT NULL,

                          department_id BIGINT,
                          designation_id BIGINT,

                          home_branch_id BIGINT,

                          reporting_manager_id BIGINT,

                          status VARCHAR(20) NOT NULL DEFAULT 'INVITED',

                          auth_provider VARCHAR(30) NOT NULL DEFAULT 'LOCAL',
                          auth_provider_user_id VARCHAR(255),

                          invitation_token VARCHAR(255),
                          invitation_expires_at TIMESTAMP,

                          password_changed_at TIMESTAMP,
                          last_login_at TIMESTAMP,

                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          created_by BIGINT,
                          updated_at TIMESTAMP,
                          updated_by BIGINT,

                          is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
                          deleted_at TIMESTAMP,
                          deleted_by BIGINT,

                          CONSTRAINT fk_user_tenant
                              FOREIGN KEY (tenant_id)
                                  REFERENCES insurance_company(id),

                          CONSTRAINT fk_user_role
                              FOREIGN KEY (role_id)
                                  REFERENCES role(id),

                          CONSTRAINT fk_user_department
                              FOREIGN KEY (department_id)
                                  REFERENCES department(id),

                          CONSTRAINT fk_user_designation
                              FOREIGN KEY (designation_id)
                                  REFERENCES designation(id),

                          CONSTRAINT fk_user_home_branch
                              FOREIGN KEY (home_branch_id)
                                  REFERENCES branch(id),

                          CONSTRAINT fk_user_reporting_manager
                              FOREIGN KEY (reporting_manager_id)
                                  REFERENCES app_user(id),

                          CONSTRAINT chk_user_status
                                  CHECK (
                                  status IN (
                                  'INVITED',
                                  'ACTIVE',
                                  'SUSPENDED',
                                  'TERMINATED'
                                  )
                                  ),

                          CONSTRAINT chk_auth_provider
                                  CHECK (
                                  auth_provider IN (
                                  'LOCAL',
                                  'GOOGLE',
                                  'AZURE_AD'
                                  )
                                  )
);

CREATE INDEX idx_app_user_email
    ON app_user (email);

CREATE UNIQUE INDEX uq_user_tenant_email
    ON app_user (tenant_id, email)
    WHERE is_deleted = FALSE;

CREATE UNIQUE INDEX uq_user_tenant_employee_code
    ON app_user (tenant_id, employee_code)
    WHERE is_deleted = FALSE;

-- =====================================================
-- User Branch Assignment
-- =====================================================

CREATE TABLE user_branch_assignment (
                                        id BIGSERIAL PRIMARY KEY,

                                        tenant_id BIGINT NOT NULL,

                                        user_id BIGINT NOT NULL,
                                        branch_id BIGINT NOT NULL,

                                        is_primary BOOLEAN NOT NULL DEFAULT FALSE,

                                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        created_by BIGINT,
                                        updated_at TIMESTAMP,
                                        updated_by BIGINT,

                                        CONSTRAINT fk_user_branch_assignment_tenant
                                            FOREIGN KEY (tenant_id)
                                                REFERENCES insurance_company(id),

                                        CONSTRAINT fk_user_branch_assignment_user
                                            FOREIGN KEY (user_id)
                                                REFERENCES app_user(id),

                                        CONSTRAINT fk_user_branch_assignment_branch
                                            FOREIGN KEY (branch_id)
                                                REFERENCES branch(id)
);

CREATE UNIQUE INDEX uq_user_branch_assignment
    ON user_branch_assignment (user_id, branch_id);


CREATE INDEX idx_app_user_tenant
    ON app_user (tenant_id);

CREATE INDEX idx_app_user_role
    ON app_user (role_id);

CREATE INDEX idx_app_user_home_branch
    ON app_user (home_branch_id);

CREATE INDEX idx_app_user_reporting_manager
    ON app_user (reporting_manager_id);

CREATE INDEX idx_user_branch_assignment_user
    ON user_branch_assignment (user_id);

CREATE INDEX idx_user_branch_assignment_branch
    ON user_branch_assignment (branch_id);

CREATE INDEX idx_department_tenant
    ON department (tenant_id);

CREATE INDEX idx_designation_tenant
    ON designation (tenant_id);

CREATE INDEX idx_app_user_invitation_token
    ON app_user(invitation_token);