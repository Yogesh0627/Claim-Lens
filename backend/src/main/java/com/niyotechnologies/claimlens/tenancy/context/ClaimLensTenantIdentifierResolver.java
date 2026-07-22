package com.niyotechnologies.claimlens.tenancy.context;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

/**
 * Feeds Hibernate the current tenant for @TenantId (discriminator) multi-tenancy.
 *
 * Must never return null — Hibernate would throw and break startup/login. When no tenant
 * is bound we return {@link TenantContext#SYSTEM_TENANT}, which matches no real tenant's rows.
 */
@Component
public class ClaimLensTenantIdentifierResolver
        implements CurrentTenantIdentifierResolver<Long> {

    @Override
    public Long resolveCurrentTenantIdentifier() {
        Long tenantId = TenantContext.getTenantIdOrNull();
        return tenantId != null ? tenantId : TenantContext.SYSTEM_TENANT;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }

    /**
     * Always false. A "root" tenant makes Hibernate skip the tenant restriction entirely —
     * a built-in backdoor we never want wired to a super-admin flag by accident.
     */
    @Override
    public boolean isRoot(Long tenantId) {
        return false;
    }
}
