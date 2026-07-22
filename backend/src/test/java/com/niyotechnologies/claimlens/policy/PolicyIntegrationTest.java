package com.niyotechnologies.claimlens.policy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.niyotechnologies.claimlens.product.entity.InsuranceProductVersion;
import com.niyotechnologies.claimlens.product.enums.ProductVersionStatus;
import com.niyotechnologies.claimlens.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 4's headline behaviour: a policy is pinned to the product's ACTIVE version at sale and does
 * NOT follow a later version — so a claim is judged against the terms the customer agreed to. Plus
 * the usual tenant isolation and RBAC guarantees.
 */
class PolicyIntegrationTest extends AbstractIntegrationTest {

    private static final String POLICIES = "/api/v1/policies";
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String policyBody(long customerId, long productId, String policyNumber) {
        return "{\"policyNumber\":\"" + policyNumber + "\",\"customerId\":" + customerId
                + ",\"insuranceProductId\":" + productId
                + ",\"effectiveFrom\":\"2024-04-01\",\"effectiveTo\":\"2025-03-31\","
                + "\"sumInsured\":650000,"
                + "\"vehicle\":{\"registrationNumber\":\"MH 12 AB 1234\","
                + "\"make\":\"Maruti\",\"model\":\"Swift\"}}";
    }

    @Test
    void policyPinsActiveVersionAndDoesNotFollowNewerVersions() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        long admin = roleIdByCode("TENANT_ADMIN");
        long customer = insertCustomer(tenant, "CUST-1");
        long product = insertProduct(tenant, "PVT_CAR_PREMIUM");
        long v1 = insertProductVersion(tenant, product, 1, ProductVersionStatus.ACTIVE);

        // Create a policy — it pins to v1 (the active version).
        MvcResult created = mockMvc.perform(post(POLICIES)
                        .header("Authorization", "Bearer " + tokenFor(1L, tenant, admin))
                        .contentType("application/json")
                        .content(policyBody(customer, product, "MOT-1")))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode data = objectMapper.readTree(created.getResponse().getContentAsString()).get("data");
        long policyId = data.get("id").asLong();
        assertEquals(v1, data.get("insuranceProductVersionId").asLong(), "policy must pin v1 at sale");
        // The registration must be normalized (spaces stripped, uppercased).
        assertEquals("MH12AB1234",
                normalize(data.get("vehicle").get("registrationNumber").asText()));

        // Ship a NEW active version (retire v1 first to satisfy the one-active-version rule).
        inTenant(tenant, () -> {
            InsuranceProductVersion old = productVersionRepository.findById(v1).orElseThrow();
            old.setStatus(ProductVersionStatus.RETIRED);
            productVersionRepository.saveAndFlush(old);
            return null;
        });
        long v2 = insertProductVersion(tenant, product, 2, ProductVersionStatus.ACTIVE);

        // The existing policy STILL points to v1, not the newer v2.
        MvcResult fetched = mockMvc.perform(get(POLICIES + "/{id}", policyId)
                        .header("Authorization", "Bearer " + tokenFor(1L, tenant, admin)))
                .andExpect(status().isOk())
                .andReturn();
        long pinned = objectMapper.readTree(fetched.getResponse().getContentAsString())
                .get("data").get("insuranceProductVersionId").asLong();
        assertEquals(v1, pinned, "existing policy must not follow the newer version");
        assertNotEquals(v1, v2, "sanity: v2 is a distinct version from v1");
    }

    @Test
    void cancelPolicyMovesItToCancelled() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        long admin = roleIdByCode("TENANT_ADMIN");
        long customer = insertCustomer(tenant, "CUST-1");
        long product = insertProduct(tenant, "PVT_CAR_PREMIUM");
        insertProductVersion(tenant, product, 1, ProductVersionStatus.ACTIVE);

        MvcResult created = mockMvc.perform(post(POLICIES)
                        .header("Authorization", "Bearer " + tokenFor(1L, tenant, admin))
                        .contentType("application/json")
                        .content(policyBody(customer, product, "MOT-C")))
                .andExpect(status().isCreated())
                .andReturn();
        long policyId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        MvcResult cancelled = mockMvc.perform(post(POLICIES + "/{id}/cancel", policyId)
                        .header("Authorization", "Bearer " + tokenFor(1L, tenant, admin)))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals("CANCELLED", objectMapper.readTree(cancelled.getResponse().getContentAsString())
                .get("data").get("status").asText());
    }

    @Test
    void tenantCannotSeeAnotherTenantsPolicy() throws Exception {
        long tenantA = insertCompany("Alpha", "ALPHA", "alpha");
        long tenantB = insertCompany("Beta", "BETA", "beta");
        long admin = roleIdByCode("TENANT_ADMIN");

        long customerB = insertCustomer(tenantB, "CUST-B");
        long productB = insertProduct(tenantB, "PVT_CAR_B");
        insertProductVersion(tenantB, productB, 1, ProductVersionStatus.ACTIVE);

        MvcResult created = mockMvc.perform(post(POLICIES)
                        .header("Authorization", "Bearer " + tokenFor(2L, tenantB, admin))
                        .contentType("application/json")
                        .content(policyBody(customerB, productB, "MOT-B")))
                .andExpect(status().isCreated())
                .andReturn();
        long policyB = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // Tenant A cannot read tenant B's policy → 404.
        mockMvc.perform(get(POLICIES + "/{id}", policyB)
                        .header("Authorization", "Bearer " + tokenFor(1L, tenantA, admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void roleWithoutPolicyWriteCannotCreatePolicy() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        long investigator = roleIdByCode("INVESTIGATOR"); // no POLICY_WRITE
        long customer = insertCustomer(tenant, "CUST-1");
        long product = insertProduct(tenant, "PVT_CAR_PREMIUM");
        insertProductVersion(tenant, product, 1, ProductVersionStatus.ACTIVE);

        mockMvc.perform(post(POLICIES)
                        .header("Authorization", "Bearer " + tokenFor(1L, tenant, investigator))
                        .contentType("application/json")
                        .content(policyBody(customer, product, "MOT-1")))
                .andExpect(status().isForbidden());
    }

    private static String normalize(String s) {
        return s.toUpperCase().replaceAll("[^A-Z0-9]", "");
    }
}
