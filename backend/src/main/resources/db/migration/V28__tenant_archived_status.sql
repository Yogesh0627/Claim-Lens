-- Add ARCHIVED to the allowed insurance_company statuses.
-- The status column already permits ACTIVE / ONBOARDING / SUSPENDED (V2's chk constraint); a tenant
-- can now also be ARCHIVED (retired but retained, distinct from a soft delete). Drop-and-recreate the
-- CHECK because Postgres has no ALTER ... CHECK in place.
ALTER TABLE insurance_company DROP CONSTRAINT IF EXISTS chk_insurance_company_status;

ALTER TABLE insurance_company
    ADD CONSTRAINT chk_insurance_company_status
        CHECK (status IN ('ACTIVE', 'ONBOARDING', 'SUSPENDED', 'ARCHIVED'));
