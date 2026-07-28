-- Demo dataset: a self-contained "Demo Insurance" tenant that tells the WHOLE org story end to end so
-- anyone signing in can follow it — tenant → regions → branches → departments → designations →
-- a reporting chain of staff (admin ▸ investigation manager ▸ investigator / adjuster / support / auditor)
-- → customers → policies + vehicles → claims across every lifecycle state → an assignment, processing
-- state and fraud scores. Idempotent (guarded on tenant_key = 'demo').
-- All demo users share the password 'Password123!'. SANDBOX ONLY — never point this at real data.
DO $$
DECLARE
    demo_tenant       BIGINT;
    -- roles
    role_tadmin       BIGINT;
    role_invmgr       BIGINT;
    role_investigator BIGINT;
    role_adjuster     BIGINT;
    role_support      BIGINT;
    role_auditor      BIGINT;
    role_platform     BIGINT;
    role_customer     BIGINT;
    motor_type        BIGINT;
    -- org units
    reg_north         BIGINT;
    reg_west          BIGINT;
    br_delhi          BIGINT;
    br_mumbai         BIGINT;
    br_blr            BIGINT;
    dep_admin         BIGINT;
    dep_claims        BIGINT;
    dep_invest        BIGINT;
    dep_support       BIGINT;
    dep_compliance    BIGINT;
    dsg_admin         BIGINT;
    dsg_regional      BIGINT;
    dsg_invmgr        BIGINT;
    dsg_srinv         BIGINT;
    dsg_adjuster      BIGINT;
    dsg_support       BIGINT;
    dsg_auditor       BIGINT;
    -- users
    u_admin           BIGINT;
    u_manager         BIGINT;
    u_investigator    BIGINT;
    u_adjuster        BIGINT;
    u_support         BIGINT;
    u_auditor         BIGINT;
    u_platform        BIGINT;
    -- customers / product / policies
    cust1             BIGINT;
    cust2             BIGINT;
    prod              BIGINT;
    ver               BIGINT;
    pol1              BIGINT;
    pol2              BIGINT;
    -- claims
    clm_draft         BIGINT;
    clm_ui            BIGINT;
    clm_appr          BIGINT;
    clm_await         BIGINT;
    clm_reject        BIGINT;
    pw                TEXT;
BEGIN
    IF EXISTS (SELECT 1 FROM insurance_company WHERE tenant_key = 'demo') THEN
        RAISE NOTICE 'Demo data already present; skipping.';
        RETURN;
    END IF;

    pw := crypt('Password123!', gen_salt('bf'));

    SELECT id INTO role_tadmin       FROM role WHERE code = 'TENANT_ADMIN';
    SELECT id INTO role_invmgr       FROM role WHERE code = 'INVESTIGATION_MANAGER';
    SELECT id INTO role_investigator FROM role WHERE code = 'INVESTIGATOR';
    SELECT id INTO role_adjuster     FROM role WHERE code = 'CLAIMS_ADJUSTER';
    SELECT id INTO role_support      FROM role WHERE code = 'CUSTOMER_SUPPORT';
    SELECT id INTO role_auditor      FROM role WHERE code = 'AUDITOR';
    SELECT id INTO role_platform     FROM role WHERE code = 'PLATFORM_ADMIN';
    SELECT id INTO role_customer     FROM role WHERE code = 'CUSTOMER';
    SELECT id INTO motor_type        FROM claim_type WHERE code = 'MOTOR';

    -- ---------------------------------------------------------------- Tenant
    INSERT INTO insurance_company (name, code, tenant_key, status, subscription_plan, currency, timezone, contact_email, head_office_address, created_at, is_deleted)
    VALUES ('Demo Insurance', 'DEMO', 'demo', 'ACTIVE', 'ENTERPRISE', 'INR', 'Asia/Kolkata',
            'contact@demo.claimlens.app', '3rd Floor, Cyber Hub, Gurugram, Haryana', now(), false)
    RETURNING id INTO demo_tenant;

    -- ---------------------------------------------------------------- Regions
    INSERT INTO region (tenant_id, code, name, status, description, created_at, is_deleted)
    VALUES (demo_tenant, 'NORTH', 'North Region', 'ACTIVE', 'Delhi NCR and northern states', now(), false)
    RETURNING id INTO reg_north;
    INSERT INTO region (tenant_id, code, name, status, description, created_at, is_deleted)
    VALUES (demo_tenant, 'WEST', 'West Region', 'ACTIVE', 'Maharashtra, Karnataka and western states', now(), false)
    RETURNING id INTO reg_west;

    -- ---------------------------------------------------------------- Branches (under regions)
    INSERT INTO branch (tenant_id, region_id, code, name, city, state, country, status, created_at, is_deleted)
    VALUES (demo_tenant, reg_north, 'DEL-HQ', 'Delhi Head Office', 'New Delhi', 'Delhi', 'India', 'ACTIVE', now(), false)
    RETURNING id INTO br_delhi;
    INSERT INTO branch (tenant_id, region_id, code, name, city, state, country, status, created_at, is_deleted)
    VALUES (demo_tenant, reg_west, 'MUM-01', 'Mumbai Branch', 'Mumbai', 'Maharashtra', 'India', 'ACTIVE', now(), false)
    RETURNING id INTO br_mumbai;
    INSERT INTO branch (tenant_id, region_id, code, name, city, state, country, status, created_at, is_deleted)
    VALUES (demo_tenant, reg_west, 'BLR-01', 'Bangalore Branch', 'Bengaluru', 'Karnataka', 'India', 'ACTIVE', now(), false)
    RETURNING id INTO br_blr;

    -- ---------------------------------------------------------------- Departments
    INSERT INTO department (tenant_id, code, name, description, status, created_at, is_deleted) VALUES
        (demo_tenant, 'ADMIN',      'Administration',      'Tenant administration and configuration', 'ACTIVE', now(), false) RETURNING id INTO dep_admin;
    INSERT INTO department (tenant_id, code, name, description, status, created_at, is_deleted) VALUES
        (demo_tenant, 'CLAIMS',     'Claims',              'Claim adjudication and settlement',       'ACTIVE', now(), false) RETURNING id INTO dep_claims;
    INSERT INTO department (tenant_id, code, name, description, status, created_at, is_deleted) VALUES
        (demo_tenant, 'INVEST',     'Investigation',       'Fraud investigation and field verification','ACTIVE', now(), false) RETURNING id INTO dep_invest;
    INSERT INTO department (tenant_id, code, name, description, status, created_at, is_deleted) VALUES
        (demo_tenant, 'SUPPORT',    'Customer Support',    'Policyholder support and communications',  'ACTIVE', now(), false) RETURNING id INTO dep_support;
    INSERT INTO department (tenant_id, code, name, description, status, created_at, is_deleted) VALUES
        (demo_tenant, 'COMPLIANCE', 'Compliance & Audit',  'Audit trail review and compliance',        'ACTIVE', now(), false) RETURNING id INTO dep_compliance;

    -- ---------------------------------------------------------------- Designations
    INSERT INTO designation (tenant_id, code, name, description, status, created_at, is_deleted) VALUES
        (demo_tenant, 'TENANT_ADMIN', 'Tenant Administrator', 'Owns tenant configuration and users', 'ACTIVE', now(), false) RETURNING id INTO dsg_admin;
    INSERT INTO designation (tenant_id, code, name, description, status, created_at, is_deleted) VALUES
        (demo_tenant, 'REGIONAL_HEAD','Regional Head',        'Oversees a region',                    'ACTIVE', now(), false) RETURNING id INTO dsg_regional;
    INSERT INTO designation (tenant_id, code, name, description, status, created_at, is_deleted) VALUES
        (demo_tenant, 'INV_MANAGER',  'Investigation Manager','Leads the investigation team',         'ACTIVE', now(), false) RETURNING id INTO dsg_invmgr;
    INSERT INTO designation (tenant_id, code, name, description, status, created_at, is_deleted) VALUES
        (demo_tenant, 'SR_INVESTIGATOR','Senior Investigator','Investigates flagged claims',          'ACTIVE', now(), false) RETURNING id INTO dsg_srinv;
    INSERT INTO designation (tenant_id, code, name, description, status, created_at, is_deleted) VALUES
        (demo_tenant, 'CLAIMS_ADJUSTER','Claims Adjuster',    'Adjudicates and settles claims',       'ACTIVE', now(), false) RETURNING id INTO dsg_adjuster;
    INSERT INTO designation (tenant_id, code, name, description, status, created_at, is_deleted) VALUES
        (demo_tenant, 'SUPPORT_EXEC', 'Support Executive',    'Handles policyholder queries',         'ACTIVE', now(), false) RETURNING id INTO dsg_support;
    INSERT INTO designation (tenant_id, code, name, description, status, created_at, is_deleted) VALUES
        (demo_tenant, 'AUDITOR',      'Compliance Auditor',   'Reviews audit trail and decisions',    'ACTIVE', now(), false) RETURNING id INTO dsg_auditor;

    -- ---------------------------------------------------------------- Staff (one per role), placed in the org
    -- Reporting chain: Tara (admin) ▸ Manoj (investigation manager) ▸ Ravi / Anil ; Sunil & Asha ▸ Tara.
    -- Admin first (top of the chain — everyone else can report up to them).
    INSERT INTO app_user (tenant_id, employee_code, first_name, last_name, email, phone, password_hash, role_id,
                          department_id, designation_id, home_branch_id, region_id, status, auth_provider, password_changed_at, created_at, is_deleted)
    VALUES (demo_tenant, 'DEMO-TA', 'Tara', 'Admin', 'admin@demo.claimlens.app', '9810000001', pw, role_tadmin,
            dep_admin, dsg_admin, br_delhi, reg_north, 'ACTIVE', 'LOCAL', now(), now(), false)
    RETURNING id INTO u_admin;

    INSERT INTO app_user (tenant_id, employee_code, first_name, last_name, email, phone, password_hash, role_id,
                          department_id, designation_id, home_branch_id, region_id, reporting_manager_id, status, auth_provider, password_changed_at, created_at, is_deleted)
    VALUES (demo_tenant, 'DEMO-IM', 'Manoj', 'Manager', 'manager@demo.claimlens.app', '9810000002', pw, role_invmgr,
            dep_invest, dsg_invmgr, br_delhi, reg_north, u_admin, 'ACTIVE', 'LOCAL', now(), now(), false)
    RETURNING id INTO u_manager;

    INSERT INTO app_user (tenant_id, employee_code, first_name, last_name, email, phone, password_hash, role_id,
                          department_id, designation_id, home_branch_id, region_id, reporting_manager_id, status, auth_provider, password_changed_at, created_at, is_deleted)
    VALUES (demo_tenant, 'DEMO-INV', 'Ravi', 'Investigator', 'investigator@demo.claimlens.app', '9810000003', pw, role_investigator,
            dep_invest, dsg_srinv, br_mumbai, reg_west, u_manager, 'ACTIVE', 'LOCAL', now(), now(), false)
    RETURNING id INTO u_investigator;

    INSERT INTO app_user (tenant_id, employee_code, first_name, last_name, email, phone, password_hash, role_id,
                          department_id, designation_id, home_branch_id, region_id, reporting_manager_id, status, auth_provider, password_changed_at, created_at, is_deleted)
    VALUES (demo_tenant, 'DEMO-CA', 'Anil', 'Adjuster', 'employee@demo.claimlens.app', '9810000004', pw, role_adjuster,
            dep_claims, dsg_adjuster, br_blr, reg_west, u_manager, 'ACTIVE', 'LOCAL', now(), now(), false)
    RETURNING id INTO u_adjuster;

    INSERT INTO app_user (tenant_id, employee_code, first_name, last_name, email, phone, password_hash, role_id,
                          department_id, designation_id, home_branch_id, region_id, reporting_manager_id, status, auth_provider, password_changed_at, created_at, is_deleted)
    VALUES (demo_tenant, 'DEMO-CS', 'Sunil', 'Support', 'support@demo.claimlens.app', '9810000005', pw, role_support,
            dep_support, dsg_support, br_delhi, reg_north, u_admin, 'ACTIVE', 'LOCAL', now(), now(), false)
    RETURNING id INTO u_support;

    INSERT INTO app_user (tenant_id, employee_code, first_name, last_name, email, phone, password_hash, role_id,
                          department_id, designation_id, home_branch_id, region_id, reporting_manager_id, status, auth_provider, password_changed_at, created_at, is_deleted)
    VALUES (demo_tenant, 'DEMO-AU', 'Asha', 'Auditor', 'auditor@demo.claimlens.app', '9810000006', pw, role_auditor,
            dep_compliance, dsg_auditor, br_delhi, reg_north, u_admin, 'ACTIVE', 'LOCAL', now(), now(), false)
    RETURNING id INTO u_auditor;

    -- Platform admin is a GLOBAL role (cross-tenant console); it isn't placed inside a tenant's org units.
    INSERT INTO app_user (tenant_id, employee_code, first_name, last_name, email, phone, password_hash, role_id,
                          status, auth_provider, password_changed_at, created_at, is_deleted)
    VALUES (demo_tenant, 'DEMO-PA', 'Priya', 'Platform', 'platformadmin@demo.claimlens.app', '9810000007', pw, role_platform,
            'ACTIVE', 'LOCAL', now(), now(), false)
    RETURNING id INTO u_platform;

    -- Region / branch ownership now that the users exist.
    UPDATE region SET owner_user_id = u_admin   WHERE id = reg_north;
    UPDATE region SET owner_user_id = u_manager  WHERE id = reg_west;
    UPDATE branch SET owner_user_id = u_admin   WHERE id = br_delhi;
    UPDATE branch SET owner_user_id = u_investigator WHERE id = br_mumbai;
    UPDATE branch SET owner_user_id = u_adjuster WHERE id = br_blr;

    -- Branch assignments (home branch is primary; Ravi covers two branches to show multi-branch placement).
    INSERT INTO user_branch_assignment (tenant_id, user_id, branch_id, is_primary, created_at, is_deleted) VALUES
        (demo_tenant, u_admin,        br_delhi,  true,  now(), false),
        (demo_tenant, u_manager,      br_delhi,  true,  now(), false),
        (demo_tenant, u_investigator, br_mumbai, true,  now(), false),
        (demo_tenant, u_investigator, br_blr,    false, now(), false),
        (demo_tenant, u_adjuster,     br_blr,    true,  now(), false),
        (demo_tenant, u_support,      br_delhi,  true,  now(), false),
        (demo_tenant, u_auditor,      br_delhi,  true,  now(), false);

    -- ---------------------------------------------------------------- Customers
    INSERT INTO customer (tenant_id, public_id, customer_number, first_name, last_name, email, phone, status, created_at, is_deleted)
    VALUES (demo_tenant, gen_random_uuid(), 'DEMO-CUST-1', 'Rahul', 'Sharma', 'rahul@example.com', '9990000001', 'ACTIVE', now(), false)
    RETURNING id INTO cust1;
    INSERT INTO customer (tenant_id, public_id, customer_number, first_name, last_name, email, phone, status, created_at, is_deleted)
    VALUES (demo_tenant, gen_random_uuid(), 'DEMO-CUST-2', 'Neha', 'Verma', 'neha@example.com', '9990000002', 'ACTIVE', now(), false)
    RETURNING id INTO cust2;

    -- Customer self-service login: a CUSTOMER-role user bound to Rahul (cust1). Sees only his own
    -- policies/claims via the ownership-scoped /portal endpoints. Uses a REAL inbox so the demo's
    -- claim-submitted / decision emails actually land during a walkthrough.
    INSERT INTO app_user (tenant_id, employee_code, first_name, last_name, email, password_hash, role_id, customer_id, status, auth_provider, password_changed_at, created_at, is_deleted)
    VALUES (demo_tenant, 'DEMO-CUR', 'Rahul', 'Sharma', 'chauhanyogesh950+rahul@gmail.com', pw, role_customer, cust1, 'ACTIVE', 'LOCAL', now(), now(), false);

    -- ---------------------------------------------------------------- Product + policies + vehicles
    INSERT INTO insurance_product (tenant_id, claim_type_id, code, name, description, status, created_at, is_deleted)
    VALUES (demo_tenant, motor_type, 'DEMO_MOTOR', 'Demo Motor Comprehensive', 'Comprehensive private car cover', 'ACTIVE', now(), false)
    RETURNING id INTO prod;
    INSERT INTO insurance_product_version (tenant_id, insurance_product_id, version_number, status, effective_from, coverage_summary, created_at, is_deleted)
    VALUES (demo_tenant, prod, 1, 'ACTIVE', DATE '2024-01-01', 'Own damage, third-party, theft and glass cover.', now(), false)
    RETURNING id INTO ver;

    INSERT INTO insurance_policy (tenant_id, public_id, policy_number, customer_id, insurance_product_id, insurance_product_version_id, effective_from, effective_to, sum_insured, deductible, currency, status, created_at, is_deleted)
    VALUES (demo_tenant, gen_random_uuid(), 'DEMO-POL-1', cust1, prod, ver, DATE '2024-04-01', DATE '2025-03-31', 650000, 1000, 'INR', 'ACTIVE', now(), false)
    RETURNING id INTO pol1;
    INSERT INTO insurance_policy (tenant_id, public_id, policy_number, customer_id, insurance_product_id, insurance_product_version_id, effective_from, effective_to, sum_insured, deductible, currency, status, created_at, is_deleted)
    VALUES (demo_tenant, gen_random_uuid(), 'DEMO-POL-2', cust2, prod, ver, DATE '2024-04-01', DATE '2025-03-31', 480000, 1000, 'INR', 'ACTIVE', now(), false)
    RETURNING id INTO pol2;

    INSERT INTO insured_vehicle (tenant_id, insurance_policy_id, registration_number, registration_number_normalized, make, model, status, created_at, is_deleted) VALUES
        (demo_tenant, pol1, 'MH 12 AB 1234', 'MH12AB1234', 'Maruti', 'Swift', 'ACTIVE', now(), false),
        (demo_tenant, pol2, 'KA 01 CD 5678', 'KA01CD5678', 'Hyundai', 'i20', 'ACTIVE', now(), false);

    -- ---------------------------------------------------------------- Claims across the lifecycle
    -- 1) DRAFT — Rahul has started a claim but not submitted it yet (visible in his portal).
    INSERT INTO claim (tenant_id, public_id, claim_number, customer_id, insurance_policy_id, insurance_product_id, insurance_product_version_id, claim_type_id, policy_number, vehicle_registration_number, incident_date, claim_amount, status, created_at, is_deleted)
    VALUES (demo_tenant, gen_random_uuid(), 'DEMO-CLM-1001', cust1, pol1, prod, ver, motor_type, 'DEMO-POL-1', 'MH12AB1234', DATE '2024-06-10', 45000, 'DRAFT', now(), false)
    RETURNING id INTO clm_draft;

    -- 2) UNDER_INVESTIGATION — flagged HIGH risk, assigned to Ravi by Manoj.
    INSERT INTO claim (tenant_id, public_id, claim_number, customer_id, insurance_policy_id, insurance_product_id, insurance_product_version_id, claim_type_id, policy_number, vehicle_registration_number, incident_date, claim_amount, status, submitted_at, created_at, is_deleted)
    VALUES (demo_tenant, gen_random_uuid(), 'DEMO-CLM-1002', cust1, pol1, prod, ver, motor_type, 'DEMO-POL-1', 'MH12AB1234', DATE '2024-06-15', 120000, 'UNDER_INVESTIGATION', now(), now(), false)
    RETURNING id INTO clm_ui;

    -- 3) APPROVED — clean claim, settled.
    INSERT INTO claim (tenant_id, public_id, claim_number, customer_id, insurance_policy_id, insurance_product_id, insurance_product_version_id, claim_type_id, policy_number, vehicle_registration_number, incident_date, claim_amount, status, submitted_at, approved_at, created_at, is_deleted)
    VALUES (demo_tenant, gen_random_uuid(), 'DEMO-CLM-1003', cust2, pol2, prod, ver, motor_type, 'DEMO-POL-2', 'KA01CD5678', DATE '2024-05-20', 30000, 'APPROVED', now(), now(), now(), false)
    RETURNING id INTO clm_appr;

    -- 4) AWAITING_ASSIGNMENT — processed, waiting for a manager to assign an investigator.
    INSERT INTO claim (tenant_id, public_id, claim_number, customer_id, insurance_policy_id, insurance_product_id, insurance_product_version_id, claim_type_id, policy_number, vehicle_registration_number, incident_date, claim_amount, status, submitted_at, created_at, is_deleted)
    VALUES (demo_tenant, gen_random_uuid(), 'DEMO-CLM-1004', cust2, pol2, prod, ver, motor_type, 'DEMO-POL-2', 'KA01CD5678', DATE '2024-06-18', 80000, 'AWAITING_ASSIGNMENT', now(), now(), false)
    RETURNING id INTO clm_await;

    -- 5) REJECTED — investigated and declined, so the "rejected" path is visible too.
    INSERT INTO claim (tenant_id, public_id, claim_number, customer_id, insurance_policy_id, insurance_product_id, insurance_product_version_id, claim_type_id, policy_number, vehicle_registration_number, incident_date, claim_amount, status, submitted_at, rejected_at, created_at, is_deleted)
    VALUES (demo_tenant, gen_random_uuid(), 'DEMO-CLM-1005', cust1, pol1, prod, ver, motor_type, 'DEMO-POL-1', 'MH12AB1234', DATE '2024-07-02', 200000, 'REJECTED', now(), now(), now(), false)
    RETURNING id INTO clm_reject;

    -- The under-investigation claim is assigned to Ravi (investigator) by Manoj (manager).
    INSERT INTO claim_assignment (tenant_id, claim_id, investigator_user_id, assigned_by, status, assigned_at)
    VALUES (demo_tenant, clm_ui, u_investigator, u_manager, 'ASSIGNED', now());
    -- The rejected claim was also investigated by Ravi (now closed).
    INSERT INTO claim_assignment (tenant_id, claim_id, investigator_user_id, assigned_by, status, assigned_at)
    VALUES (demo_tenant, clm_reject, u_investigator, u_manager, 'COMPLETED', now());

    -- Processing state + fraud scores so the Processing/Analytics screens are populated.
    INSERT INTO claim_processing_state (tenant_id, claim_id, ocr_status, analysis_status, fraud_status, pending_reprocess, last_updated_at, created_at) VALUES
        (demo_tenant, clm_ui,     'COMPLETE', 'COMPLETE', 'COMPLETE', false, now(), now()),
        (demo_tenant, clm_appr,   'COMPLETE', 'COMPLETE', 'COMPLETE', false, now(), now()),
        (demo_tenant, clm_await,  'COMPLETE', 'COMPLETE', 'COMPLETE', false, now(), now()),
        (demo_tenant, clm_reject, 'COMPLETE', 'COMPLETE', 'COMPLETE', false, now(), now());

    INSERT INTO fraud_score (tenant_id, claim_id, score, risk_level, explanation, created_at) VALUES
        (demo_tenant, clm_ui,     72, 'HIGH',   'Claim amount high vs. segment baseline; early claim after policy start.', now()),
        (demo_tenant, clm_appr,   12, 'LOW',    'No fraud signals triggered.', now()),
        (demo_tenant, clm_await,  41, 'MEDIUM', 'Repeat claim on the same vehicle within the policy year.', now()),
        (demo_tenant, clm_reject, 88, 'HIGH',   'Amount exceeds sum insured; duplicate damage photo reused from an earlier claim.', now());

    RAISE NOTICE 'Demo data seeded for tenant % (regions, branches, departments, designations, staff chain, customers, claims).', demo_tenant;
END $$;
