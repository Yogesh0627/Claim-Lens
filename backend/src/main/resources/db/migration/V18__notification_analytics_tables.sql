-- =====================================================
-- V18__notification_analytics_tables.sql
-- In-app notifications (email delivery deferred) + the analytics-read permission. Analytics needs no
-- new tables — dashboards are aggregate queries over claim / fraud_score.
-- =====================================================

CREATE TABLE notification (
    id                BIGSERIAL PRIMARY KEY,
    tenant_id         BIGINT NOT NULL,
    recipient_user_id BIGINT NOT NULL,

    type              VARCHAR(50) NOT NULL,
    title             VARCHAR(255) NOT NULL,
    message           TEXT,
    is_read           BOOLEAN NOT NULL DEFAULT FALSE,

    created_at        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_notification_tenant    FOREIGN KEY (tenant_id) REFERENCES insurance_company(id),
    CONSTRAINT fk_notification_recipient FOREIGN KEY (recipient_user_id) REFERENCES app_user(id)
);
CREATE INDEX idx_notification_recipient ON notification (tenant_id, recipient_user_id, is_read);

INSERT INTO permission (code, name, module) VALUES
    ('ANALYTICS_READ', 'View analytics dashboards', 'ANALYTICS');

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.code IN ('TENANT_ADMIN', 'INVESTIGATION_MANAGER', 'AUDITOR', 'ANALYST')
  AND p.code = 'ANALYTICS_READ';
