package com.niyotechnologies.claimlens.tenancy;

import com.niyotechnologies.claimlens.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The reason the whole foundation exists. Data is seeded via raw JDBC (bypassing the tenant
 * filter), then read over HTTP as a specific tenant. The load-bearing assertion is that
 * tenant A gets 404 — not 200, not 403 — on tenant B's region.
 *
 * The tenant is derived from the JWT (never a path variable); the URLs carry no companyId.
 */
class TenantIsolationIntegrationTest extends AbstractIntegrationTest {

    private static final long ROLE_ID = 1L;
    private static final String REGIONS = "/api/v1/organizations/regions";

    @Test
    void tenantCannotReadAnotherTenantsRegion() throws Exception {
        long tenantA = insertCompany("Alpha Insurance", "ALPHA", "alpha");
        long tenantB = insertCompany("Beta Insurance", "BETA", "beta");
        long regionA = insertRegion(tenantA, "RA", "Region Alpha");
        long regionB = insertRegion(tenantB, "RB", "Region Beta");

        String tokenA = tokenFor(1L, tenantA, ROLE_ID);

        // Own region → 200
        mockMvc.perform(get(REGIONS + "/{r}", regionA)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        // Another tenant's region → 404 (existence must NOT leak as 200 or 403)
        mockMvc.perform(get(REGIONS + "/{r}", regionB)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }

    @Test
    void listReturnsOnlyOwnTenantRegions() throws Exception {
        long tenantA = insertCompany("Alpha Insurance", "ALPHA", "alpha");
        long tenantB = insertCompany("Beta Insurance", "BETA", "beta");
        insertRegion(tenantA, "RA", "Region Alpha");
        insertRegion(tenantB, "RB", "Region Beta");

        mockMvc.perform(get(REGIONS)
                        .header("Authorization", "Bearer " + tokenFor(1L, tenantA, ROLE_ID)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Region Alpha")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Region Beta"))));
    }

    @Test
    void createStampsTheAuthenticatedTenantAndStaysIsolated() throws Exception {
        long tenantA = insertCompany("Alpha Insurance", "ALPHA", "alpha");
        long tenantB = insertCompany("Beta Insurance", "BETA", "beta");

        // Create as tenant A — Hibernate @TenantId must stamp tenant_id = A (not set by the client).
        mockMvc.perform(post(REGIONS)
                        .header("Authorization", "Bearer " + tokenFor(1L, tenantA, ROLE_ID))
                        .contentType("application/json")
                        .content("{\"code\":\"WEST\",\"name\":\"Western Region\",\"status\":\"ACTIVE\"}"))
                .andExpect(status().isCreated());

        // Tenant B must not see A's newly created region.
        mockMvc.perform(get(REGIONS)
                        .header("Authorization", "Bearer " + tokenFor(2L, tenantB, ROLE_ID)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Western Region"))));

        // Tenant A does see it.
        mockMvc.perform(get(REGIONS)
                        .header("Authorization", "Bearer " + tokenFor(1L, tenantA, ROLE_ID)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Western Region")));
    }

    @Test
    void requestWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get(REGIONS))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void algNoneTokenIsRejected() throws Exception {
        long tenantA = insertCompany("Alpha Insurance", "ALPHA", "alpha");

        Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
        String header = b64.encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
        String payload = b64.encodeToString(
                ("{\"sub\":\"1\",\"tid\":" + tenantA + ",\"rid\":1}").getBytes(StandardCharsets.UTF_8));
        String unsignedToken = header + "." + payload + ".";

        mockMvc.perform(get(REGIONS)
                        .header("Authorization", "Bearer " + unsignedToken))
                .andExpect(status().isUnauthorized());
    }
}
