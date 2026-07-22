package com.niyotechnologies.claimlens.platform.service;

import com.niyotechnologies.claimlens.platform.dto.PlatformAnalyticsResponse;
import com.niyotechnologies.claimlens.platform.dto.PlatformAnalyticsResponse.GrowthPoint;
import com.niyotechnologies.claimlens.platform.dto.PlatformAnalyticsResponse.TenantStat;
import com.niyotechnologies.claimlens.platform.dto.PlatformAnalyticsResponse.Totals;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Cross-tenant platform analytics. Uses JdbcTemplate (native SQL) ON PURPOSE — @TenantId would scope
 * every query to one tenant, but the platform admin needs aggregates across all of them. This is the
 * same deliberate bypass PermissionService uses for the global role/permission tables.
 */
@Service
@RequiredArgsConstructor
public class PlatformAnalyticsService {

    @Autowired
    private final JdbcTemplate jdbc;

    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public PlatformAnalyticsResponse analytics() {
        return new PlatformAnalyticsResponse(totals(), tenants(), growth());
    }

    private Totals totals() {
        return jdbc.queryForObject(
                """
                SELECT
                  (SELECT count(*) FROM insurance_company WHERE is_deleted = false) AS tenants,
                  (SELECT count(*) FROM insurance_company WHERE is_deleted = false AND status = 'ACTIVE') AS active,
                  (SELECT count(*) FROM insurance_company WHERE is_deleted = false AND status = 'ONBOARDING') AS onboarding,
                  (SELECT count(*) FROM insurance_company WHERE is_deleted = false AND status = 'SUSPENDED') AS suspended,
                  (SELECT count(*) FROM app_user WHERE is_deleted = false) AS users,
                  (SELECT count(*) FROM claim WHERE is_deleted = false) AS claims,
                  (SELECT count(*) FROM insurance_policy) AS policies
                """,
                (rs, n) -> new Totals(
                        rs.getLong("tenants"), rs.getLong("active"), rs.getLong("onboarding"),
                        rs.getLong("suspended"), rs.getLong("users"), rs.getLong("claims"),
                        rs.getLong("policies")));
    }

    private List<TenantStat> tenants() {
        return jdbc.query(
                """
                SELECT ic.id, ic.name, ic.code, ic.status, ic.subscription_plan, ic.currency, ic.created_at,
                  (SELECT count(*) FROM app_user u WHERE u.tenant_id = ic.id AND u.is_deleted = false) AS users,
                  (SELECT count(*) FROM claim c WHERE c.tenant_id = ic.id AND c.is_deleted = false) AS claims,
                  (SELECT count(*) FROM fraud_score f WHERE f.tenant_id = ic.id AND f.risk_level = 'HIGH') AS high,
                  (SELECT count(*) FROM fraud_score f WHERE f.tenant_id = ic.id AND f.risk_level = 'MEDIUM') AS medium,
                  (SELECT count(*) FROM fraud_score f WHERE f.tenant_id = ic.id AND f.risk_level = 'LOW') AS low
                FROM insurance_company ic
                WHERE ic.is_deleted = false
                ORDER BY ic.created_at
                """,
                (rs, n) -> new TenantStat(
                        rs.getLong("id"), rs.getString("name"), rs.getString("code"),
                        rs.getString("status"), rs.getString("subscription_plan"), rs.getString("currency"),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getLong("users"), rs.getLong("claims"),
                        rs.getLong("high"), rs.getLong("medium"), rs.getLong("low")));
    }

    private List<GrowthPoint> growth() {
        return jdbc.query(
                """
                SELECT to_char(m, 'YYYY-MM') AS month,
                  (SELECT count(*) FROM insurance_company ic
                     WHERE ic.is_deleted = false AND date_trunc('month', ic.created_at) = m) AS new_tenants,
                  (SELECT count(*) FROM claim c
                     WHERE c.is_deleted = false AND date_trunc('month', c.created_at) = m) AS new_claims
                FROM generate_series(
                       date_trunc('month', now()) - interval '5 months',
                       date_trunc('month', now()),
                       interval '1 month') AS m
                ORDER BY month
                """,
                (rs, n) -> new GrowthPoint(
                        rs.getString("month"), rs.getLong("new_tenants"), rs.getLong("new_claims")));
    }
}
