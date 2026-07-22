-- =====================================================
-- V6__auth_tables.sql
-- Refresh-token sessions. A refresh token is an opaque random string; only its SHA-256 hash is
-- stored here, never the raw token. Rotation links each session to its replacement so reuse of an
-- already-rotated token can be detected (theft signal).
-- =====================================================

CREATE TABLE user_session (
    id                     BIGSERIAL PRIMARY KEY,
    tenant_id              BIGINT NOT NULL,
    user_id                BIGINT NOT NULL,

    refresh_token_hash     VARCHAR(64) NOT NULL,   -- SHA-256 hex

    expires_at             TIMESTAMPTZ NOT NULL,
    revoked_at             TIMESTAMPTZ,
    replaced_by_session_id BIGINT,

    user_agent             VARCHAR(512),
    ip_address             VARCHAR(64),

    created_at             TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_session_tenant
        FOREIGN KEY (tenant_id) REFERENCES insurance_company(id),
    CONSTRAINT fk_user_session_user
        FOREIGN KEY (user_id) REFERENCES app_user(id),
    CONSTRAINT fk_user_session_replaced_by
        FOREIGN KEY (replaced_by_session_id) REFERENCES user_session(id)
);

CREATE UNIQUE INDEX uq_user_session_token ON user_session (refresh_token_hash);
CREATE INDEX idx_user_session_user ON user_session (user_id);
CREATE INDEX idx_user_session_tenant ON user_session (tenant_id);
