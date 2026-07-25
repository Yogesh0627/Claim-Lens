package com.niyotechnologies.claimlens.tenancy;

import com.niyotechnologies.claimlens.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    private static final String COMPANIES = "/api/v1/organizations/companies";

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

    // ── InsuranceCompany: the tenant ROOT ────────────────────────────────────────────────────────
    // These exist because company is the one aggregate @TenantId does NOT protect (it has no
    // discriminator — you must be able to load it before the tenant is known). Guarding it was left
    // to ORG_COMPANY_READ/WRITE, which TENANT_ADMIN and AUDITOR also hold, so any tenant admin could
    // read AND overwrite every other tenant's company. Verified against the running app before the
    // fix: a rename of another tenant's company returned 200 and persisted.

    @Test
    void tenantAdminCannotListEveryTenantsCompany() throws Exception {
        insertCompany("Alpha Insurance", "ALPHA", "alpha");
        long tenantB = insertCompany("Beta Insurance", "BETA", "beta");

        mockMvc.perform(get(COMPANIES)
                        .header("Authorization", "Bearer " + tokenFor(1L, tenantB, roleIdByCode("TENANT_ADMIN"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void auditorCannotListEveryTenantsCompany() throws Exception {
        insertCompany("Alpha Insurance", "ALPHA", "alpha");
        long tenantB = insertCompany("Beta Insurance", "BETA", "beta");

        mockMvc.perform(get(COMPANIES)
                        .header("Authorization", "Bearer " + tokenFor(1L, tenantB, roleIdByCode("AUDITOR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void tenantAdminCannotReadAnotherTenantsCompany() throws Exception {
        long tenantA = insertCompany("Alpha Insurance", "ALPHA", "alpha");
        long tenantB = insertCompany("Beta Insurance", "BETA", "beta");

        String tokenB = tokenFor(1L, tenantB, roleIdByCode("TENANT_ADMIN"));

        // Own company → 200
        mockMvc.perform(get(COMPANIES + "/{id}", tenantB)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk());

        // Another tenant's company → 404, never 200 and never 403 (403 confirms it exists)
        mockMvc.perform(get(COMPANIES + "/{id}", tenantA)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    void tenantAdminCannotOverwriteAnotherTenantsCompany() throws Exception {
        long tenantA = insertCompany("Alpha Insurance", "ALPHA", "alpha");
        long tenantB = insertCompany("Beta Insurance", "BETA", "beta");

        mockMvc.perform(put(COMPANIES + "/{id}", tenantA)
                        .header("Authorization", "Bearer "
                                + tokenFor(1L, tenantB, roleIdByCode("TENANT_ADMIN")))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        // Must be a fully VALID body: bean validation runs before the ownership
                        // check, so a partial payload returns 400 and never exercises the guard.
                        .content("""
                                {"name":"PWNED","subscriptionPlan":"BASIC",\
                                "currency":"INR","timezone":"Asia/Kolkata"}"""))
                .andExpect(status().isNotFound());

        // The write must not have landed — a 404 that still mutated would be the worse bug.
        org.junit.jupiter.api.Assertions.assertEquals(
                "Alpha Insurance",
                insuranceCompanyRepository.findById(tenantA).orElseThrow().getName());
    }

    @Test
    void platformAdminRetainsCrossTenantAccess() throws Exception {
        long tenantA = insertCompany("Alpha Insurance", "ALPHA", "alpha");
        insertCompany("Beta Insurance", "BETA", "beta");

        String platform = tokenFor(1L, tenantA, roleIdByCode("PLATFORM_ADMIN"));

        mockMvc.perform(get(COMPANIES).header("Authorization", "Bearer " + platform))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Beta Insurance")));
    }

    @Test
    void companiesMeResolvesOwnCompanyFromTheToken() throws Exception {
        insertCompany("Alpha Insurance", "ALPHA", "alpha");
        long tenantB = insertCompany("Beta Insurance", "BETA", "beta");

        mockMvc.perform(get(COMPANIES + "/me")
                        .header("Authorization", "Bearer " + tokenFor(1L, tenantB, roleIdByCode("TENANT_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Beta Insurance")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Alpha Insurance"))));
    }

    @Test
    void nonNumericCompanyIdIsBadRequestNotServerError() throws Exception {
        long tenantA = insertCompany("Alpha Insurance", "ALPHA", "alpha");

        // "not-a-number" cannot bind to Long. Before the type-mismatch handler this escaped to the
        // catch-all as a 500 — reporting a server fault for a malformed request. Applies to every
        // {id} route, so it is asserted once here.
        mockMvc.perform(get(COMPANIES + "/{id}", "not-a-number")
                        .header("Authorization", "Bearer " + tokenFor(1L, tenantA, ROLE_ID)))
                .andExpect(status().isBadRequest());
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
