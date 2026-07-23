-- Single-use, expiring tokens for setting a password:
--   INVITE — issued when an admin creates a user without a password (the account is INVITED until
--            the person chooses their own; before this table, INVITED was a dead end — no password
--            to sign in with, and Google sign-in requires ACTIVE).
--   RESET  — self-service "forgot password".
--
-- Only the SHA-256 hash of the token is stored, exactly as user_session does for refresh tokens: a
-- leaked database yields no usable links. The raw token exists only in the emailed URL.
--
-- Deliberately NOT tenant-aware (no tenant_id discriminator on the entity): the token is redeemed by
-- an ANONYMOUS caller, so there is no tenant in context to filter by — the same reason user_session
-- isn't tenant-scoped. tenant_id is still stored for auditing and cleanup.
CREATE TABLE user_invitation (
    id         BIGSERIAL PRIMARY KEY,
    tenant_id  BIGINT      NOT NULL,
    user_id    BIGINT      NOT NULL,

    token_hash VARCHAR(64) NOT NULL,   -- SHA-256 hex of the raw token
    purpose    VARCHAR(20) NOT NULL,

    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,            -- non-null = already redeemed (single use)

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_invitation_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT uq_user_invitation_token UNIQUE (token_hash),
    CONSTRAINT ck_user_invitation_purpose CHECK (purpose IN ('INVITE', 'RESET'))
);

-- Redemption looks up by token_hash (covered by the unique constraint). This index serves the
-- "invalidate this user's outstanding tokens" sweep performed when one is redeemed.
CREATE INDEX idx_user_invitation_user ON user_invitation (user_id);
