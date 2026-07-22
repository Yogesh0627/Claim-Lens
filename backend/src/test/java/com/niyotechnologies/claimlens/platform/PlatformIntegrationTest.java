package com.niyotechnologies.claimlens.platform;

import com.niyotechnologies.claimlens.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The platform (SaaS-operator) layer: cross-tenant analytics, tenant onboarding/suspension, and
 * impersonation. Gated by the PLATFORM_ADMIN authority; a tenant admin cannot reach it.
 */
class PlatformIntegrationTest extends AbstractIntegrationTest {

    private long platformRole() {
        return roleIdByCode("PLATFORM_ADMIN");
    }

    @Test
    void analyticsAggregatesAcrossTenants() throws Exception {
        long tenantA = insertCompany("Alpha", "ALPHA", "alpha");
        long tenantB = insertCompany("Beta", "BETA", "beta");
        String auth = "Bearer " + tokenFor(1L, tenantA, platformRole());

        mockMvc.perform(get("/api/v1/platform/analytics").header("Authorization", auth))
                .andExpect(status().isOk())
                // Both companies are counted across tenants (native, @TenantId bypassed).
                .andExpect(jsonPath("$.data.totals.tenants").value(greaterThanOrEqualTo(2)))
                .andExpect(content().string(containsString("Alpha")))
                .andExpect(content().string(containsString("Beta")));
        // silence unused warning; tenantB participates via the cross-tenant count above
        assert tenantB > 0;
    }

    @Test
    void onboardAndSuspendTenant() throws Exception {
        long home = insertCompany("Alpha", "ALPHA", "alpha");
        String auth = "Bearer " + tokenFor(1L, home, platformRole());

        String created = mockMvc.perform(post("/api/v1/platform/tenants").header("Authorization", auth)
                        .contentType("application/json")
                        .content("{\"name\":\"Gamma Insure\",\"code\":\"GAMMA\",\"tenantKey\":\"gamma\","
                                + "\"currency\":\"INR\",\"timezone\":\"Asia/Kolkata\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("ONBOARDING"))
                .andReturn().getResponse().getContentAsString();
        long tenantId = om().readTree(created).get("data").get("id").asLong();

        mockMvc.perform(post("/api/v1/platform/tenants/{id}/status", tenantId).header("Authorization", auth)
                        .contentType("application/json").content("{\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"));
    }

    @Test
    void impersonationIssuesTenantScopedToken() throws Exception {
        long tenantA = insertCompany("Alpha", "ALPHA", "alpha");
        String auth = "Bearer " + tokenFor(1L, tenantA, platformRole());

        mockMvc.perform(post("/api/v1/platform/tenants/{id}/impersonate", tenantA).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.tenantId").value((int) tenantA));
    }

    @Test
    void tenantAdminCannotReachPlatform() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        mockMvc.perform(get("/api/v1/platform/analytics")
                        .header("Authorization", "Bearer " + tokenFor(1L, tenant, roleIdByCode("TENANT_ADMIN"))))
                .andExpect(status().isForbidden());
    }

    private com.fasterxml.jackson.databind.ObjectMapper om() {
        return new com.fasterxml.jackson.databind.ObjectMapper();
    }
}
