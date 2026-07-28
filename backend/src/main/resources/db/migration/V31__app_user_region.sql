-- A staff user can belong to a region (e.g. a regional manager overseeing several branches).
-- Branches are the granular placement (via user_branch_assignment + home_branch_id); the region is
-- the wider org unit. Nullable — customers and unplaced staff have none.
-- Written idempotently (IF NOT EXISTS / guarded constraint) so it re-applies cleanly.
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS region_id BIGINT;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_app_user_region') THEN
        ALTER TABLE app_user
            ADD CONSTRAINT fk_app_user_region FOREIGN KEY (region_id) REFERENCES region(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_app_user_region ON app_user (region_id);

-- user_branch_assignment (created in V4) predates the soft-delete convention; the entity now maps it
-- via TenantAwareEntity, which requires these columns. Add them so schema validation passes.
ALTER TABLE user_branch_assignment ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE user_branch_assignment ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE user_branch_assignment ADD COLUMN IF NOT EXISTS deleted_by BIGINT;
