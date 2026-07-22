package com.niyotechnologies.claimlens.platform.dto;

import java.time.Instant;
import java.util.List;

/** Cross-tenant platform analytics. `tenants` doubles as the tenant directory and the by-tenant report. */
public record PlatformAnalyticsResponse(
        Totals totals,
        List<TenantStat> tenants,
        List<GrowthPoint> growth
) {

    public record Totals(
            long tenants,
            long activeTenants,
            long onboardingTenants,
            long suspendedTenants,
            long users,
            long claims,
            long policies
    ) {
    }

    public record TenantStat(
            long tenantId,
            String name,
            String code,
            String status,
            String subscriptionPlan,
            String currency,
            Instant createdAt,
            long users,
            long claims,
            long highRisk,
            long mediumRisk,
            long lowRisk
    ) {
    }

    public record GrowthPoint(String month, long newTenants, long newClaims) {
    }
}
