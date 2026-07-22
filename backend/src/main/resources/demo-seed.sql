-- Demo dataset: a self-contained "Demo Insurance" tenant with one user per role and curated data
-- (customers, product, policies, claims in various states, an assignment, fraud scores) so recruiters
-- can sign in as any role and immediately see meaningful screens. Idempotent (guarded on tenant_key).
-- All demo users share the password 'Password123!'. SANDBOX ONLY — never point this at real data.
DO $$
DECLARE
    demo_tenant       BIGINT;
    role_tadmin       BIGINT;
    role_invmgr       BIGINT;
    role_investigator BIGINT;
    role_adjuster     BIGINT;
    role_support      BIGINT;
    role_auditor      BIGINT;
    role_platform     BIGINT;
    role_customer     BIGINT;
    motor_type        BIGINT;
    u_manager         BIGINT;
    u_investigator    BIGINT;
    cust1             BIGINT;
    cust2             BIGINT;
    prod              BIGINT;
    ver               BIGINT;
    pol1              BIGINT;
    pol2              BIGINT;
    clm_ui            BIGINT;
    clm_appr          BIGINT;
    clm_await         BIGINT;
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

    INSERT INTO insurance_company (name, code, tenant_key, status, subscription_plan, currency, timezone, created_at, is_deleted)
    VALUES ('Demo Insurance', 'DEMO', 'demo', 'ACTIVE', 'ENTERPRISE', 'INR', 'Asia/Kolkata', now(), false)
    RETURNING id INTO demo_tenant;

    -- One user per role (all password 'Password123!').
    INSERT INTO app_user (tenant_id, employee_code, first_name, last_name, email, password_hash, role_id, status, auth_provider, created_at, is_deleted) VALUES
        (demo_tenant, 'DEMO-TA',  'Tara',  'Admin',       'admin@demo.claimlens.app',        pw, role_tadmin,       'ACTIVE', 'LOCAL', now(), false),
        (demo_tenant, 'DEMO-IM',  'Manoj', 'Manager',      'manager@demo.claimlens.app',      pw, role_invmgr,       'ACTIVE', 'LOCAL', now(), false),
        (demo_tenant, 'DEMO-CS',  'Sunil', 'Support',      'support@demo.claimlens.app',      pw, role_support,      'ACTIVE', 'LOCAL', now(), false),
        (demo_tenant, 'DEMO-CA',  'Anil',  'Adjuster',     'employee@demo.claimlens.app',     pw, role_adjuster,     'ACTIVE', 'LOCAL', now(), false),
        (demo_tenant, 'DEMO-AU',  'Asha',  'Auditor',      'auditor@demo.claimlens.app',      pw, role_auditor,      'ACTIVE', 'LOCAL', now(), false),
        (demo_tenant, 'DEMO-PA',  'Priya', 'Platform',     'platformadmin@demo.claimlens.app', pw, role_platform,    'ACTIVE', 'LOCAL', now(), false);
    INSERT INTO app_user (tenant_id, employee_code, first_name, last_name, email, password_hash, role_id, status, auth_provider, created_at, is_deleted)
    VALUES (demo_tenant, 'DEMO-INV', 'Ravi', 'Investigator', 'investigator@demo.claimlens.app', pw, role_investigator, 'ACTIVE', 'LOCAL', now(), false)
    RETURNING id INTO u_investigator;
    SELECT id INTO u_manager FROM app_user WHERE email = 'manager@demo.claimlens.app';

    INSERT INTO customer (tenant_id, public_id, customer_number, first_name, last_name, email, phone, status, created_at, is_deleted)
    VALUES (demo_tenant, gen_random_uuid(), 'DEMO-CUST-1', 'Rahul', 'Sharma', 'rahul@example.com', '9990000001', 'ACTIVE', now(), false)
    RETURNING id INTO cust1;
    INSERT INTO customer (tenant_id, public_id, customer_number, first_name, last_name, email, phone, status, created_at, is_deleted)
    VALUES (demo_tenant, gen_random_uuid(), 'DEMO-CUST-2', 'Neha', 'Verma', 'neha@example.com', '9990000002', 'ACTIVE', now(), false)
    RETURNING id INTO cust2;

    -- Customer self-service login: a CUSTOMER-role user bound to Rahul (cust1). Sees only his own
    -- policies/claims via the ownership-scoped /portal endpoints. Uses a REAL inbox so the demo's
    -- claim-submitted / decision emails actually land during a walkthrough.
    INSERT INTO app_user (tenant_id, employee_code, first_name, last_name, email, password_hash, role_id, customer_id, status, auth_provider, created_at, is_deleted)
    VALUES (demo_tenant, 'DEMO-CUR', 'Rahul', 'Sharma', 'chauhanyogesh950+rahul@gmail.com', pw, role_customer, cust1, 'ACTIVE', 'LOCAL', now(), false);

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

    -- Claims across the lifecycle so every role sees something useful.
    INSERT INTO claim (tenant_id, public_id, claim_number, customer_id, insurance_policy_id, insurance_product_id, insurance_product_version_id, claim_type_id, policy_number, vehicle_registration_number, incident_date, claim_amount, status, created_at, is_deleted)
    VALUES (demo_tenant, gen_random_uuid(), 'DEMO-CLM-1001', cust1, pol1, prod, ver, motor_type, 'DEMO-POL-1', 'MH12AB1234', DATE '2024-06-10', 45000, 'DRAFT', now(), false);

    INSERT INTO claim (tenant_id, public_id, claim_number, customer_id, insurance_policy_id, insurance_product_id, insurance_product_version_id, claim_type_id, policy_number, vehicle_registration_number, incident_date, claim_amount, status, submitted_at, created_at, is_deleted)
    VALUES (demo_tenant, gen_random_uuid(), 'DEMO-CLM-1002', cust1, pol1, prod, ver, motor_type, 'DEMO-POL-1', 'MH12AB1234', DATE '2024-06-15', 120000, 'UNDER_INVESTIGATION', now(), now(), false)
    RETURNING id INTO clm_ui;

    INSERT INTO claim (tenant_id, public_id, claim_number, customer_id, insurance_policy_id, insurance_product_id, insurance_product_version_id, claim_type_id, policy_number, vehicle_registration_number, incident_date, claim_amount, status, submitted_at, created_at, is_deleted)
    VALUES (demo_tenant, gen_random_uuid(), 'DEMO-CLM-1003', cust2, pol2, prod, ver, motor_type, 'DEMO-POL-2', 'KA01CD5678', DATE '2024-05-20', 30000, 'APPROVED', now(), now(), false)
    RETURNING id INTO clm_appr;

    INSERT INTO claim (tenant_id, public_id, claim_number, customer_id, insurance_policy_id, insurance_product_id, insurance_product_version_id, claim_type_id, policy_number, vehicle_registration_number, incident_date, claim_amount, status, submitted_at, created_at, is_deleted)
    VALUES (demo_tenant, gen_random_uuid(), 'DEMO-CLM-1004', cust2, pol2, prod, ver, motor_type, 'DEMO-POL-2', 'KA01CD5678', DATE '2024-06-18', 80000, 'AWAITING_ASSIGNMENT', now(), now(), false)
    RETURNING id INTO clm_await;

    -- The under-investigation claim is assigned to the demo investigator.
    INSERT INTO claim_assignment (tenant_id, claim_id, investigator_user_id, assigned_by, status, assigned_at)
    VALUES (demo_tenant, clm_ui, u_investigator, u_manager, 'ASSIGNED', now());

    -- Processing state + fraud scores so the Processing/Analytics screens are populated.
    INSERT INTO claim_processing_state (tenant_id, claim_id, ocr_status, analysis_status, fraud_status, pending_reprocess, last_updated_at, created_at) VALUES
        (demo_tenant, clm_ui,    'COMPLETE', 'COMPLETE', 'COMPLETE', false, now(), now()),
        (demo_tenant, clm_appr,  'COMPLETE', 'COMPLETE', 'COMPLETE', false, now(), now()),
        (demo_tenant, clm_await, 'COMPLETE', 'COMPLETE', 'COMPLETE', false, now(), now());

    INSERT INTO fraud_score (tenant_id, claim_id, score, risk_level, explanation, created_at) VALUES
        (demo_tenant, clm_ui,    72, 'HIGH',   'Claim amount high vs. segment baseline; early claim after policy start.', now()),
        (demo_tenant, clm_appr,  12, 'LOW',    'No fraud signals triggered.', now()),
        (demo_tenant, clm_await, 41, 'MEDIUM', 'Repeat claim on the same vehicle within the policy year.', now());

    RAISE NOTICE 'Demo data seeded for tenant %.', demo_tenant;
END $$;
