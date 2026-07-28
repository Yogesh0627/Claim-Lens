-- Per-tenant role customization. Roles and their default permissions stay global (role_permission is
-- the platform default). When a tenant customizes a role, its chosen permission set is stored here and
-- OVERRIDES the global default for that tenant only — so one company's changes never affect another's.
-- A role with no rows here for a tenant simply inherits the global default.
CREATE TABLE tenant_role_permission (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     BIGINT NOT NULL,
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,

    created_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    BIGINT,

    CONSTRAINT fk_trp_tenant     FOREIGN KEY (tenant_id) REFERENCES insurance_company(id),
    CONSTRAINT fk_trp_role       FOREIGN KEY (role_id) REFERENCES role(id),
    CONSTRAINT fk_trp_permission FOREIGN KEY (permission_id) REFERENCES permission(id),
    CONSTRAINT uq_tenant_role_permission UNIQUE (tenant_id, role_id, permission_id)
);

CREATE INDEX idx_tenant_role_permission_lookup
    ON tenant_role_permission (tenant_id, role_id);
