package com.niyotechnologies.claimlens.claim;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.niyotechnologies.claimlens.product.enums.ProductVersionStatus;
import com.niyotechnologies.claimlens.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Claim intake, demonstrating the hard-vs-soft validation philosophy (interview-prep Part 9):
 *   - HARD failures (expired policy, vehicle mismatch) REJECT the submission (400).
 *   - SOFT signals (amount over sum insured) SUBMIT anyway (200) and return warnings.
 */
class ClaimIntakeIntegrationTest extends AbstractIntegrationTest {

    private static final String POLICIES = "/api/v1/policies";
    private static final String CLAIMS = "/api/v1/claims";
    private final ObjectMapper om = new ObjectMapper();

    private String bearer(long tenant, long role) {
        return "Bearer " + tokenFor(1L, tenant, role);
    }

    /** Creates a product+active version and a policy (with vehicle MH12AB1234, sum insured 650000). */
    private long setupPolicy(long tenant, long admin, long customerId) throws Exception {
        long product = insertProduct(tenant, "PVT_CAR_PREMIUM");
        insertProductVersion(tenant, product, 1, ProductVersionStatus.ACTIVE);
        String body = "{\"policyNumber\":\"MOT-1\",\"customerId\":" + customerId
                + ",\"insuranceProductId\":" + product
                + ",\"effectiveFrom\":\"2024-04-01\",\"effectiveTo\":\"2025-03-31\","
                + "\"sumInsured\":650000,"
                + "\"vehicle\":{\"registrationNumber\":\"MH 12 AB 1234\",\"make\":\"Maruti\",\"model\":\"Swift\"}}";
        MvcResult r = mockMvc.perform(post(POLICIES).header("Authorization", bearer(tenant, admin))
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated()).andReturn();
        return om.readTree(r.getResponse().getContentAsString()).get("data").get("id").asLong();
    }

    private String claimBody(long customerId, long policyId, String incidentDate,
                             long amount, String vehicleReg) {
        return "{\"customerId\":" + customerId + ",\"insurancePolicyId\":" + policyId
                + ",\"incidentDate\":\"" + incidentDate + "\",\"claimAmount\":" + amount
                + ",\"vehicleRegistrationNumber\":\"" + vehicleReg + "\"}";
    }

    private long createDraft(long tenant, long admin, String body) throws Exception {
        MvcResult r = mockMvc.perform(post(CLAIMS).header("Authorization", bearer(tenant, admin))
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated()).andReturn();
        return om.readTree(r.getResponse().getContentAsString()).get("data").get("id").asLong();
    }

    @Test
    void validClaimSubmitsWithNoWarnings() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        long admin = roleIdByCode("TENANT_ADMIN");
        long customer = insertCustomer(tenant, "CUST-1");
        long policy = setupPolicy(tenant, admin, customer);

        long claim = createDraft(tenant, admin,
                claimBody(customer, policy, "2024-06-01", 50000, "MH-12-AB-1234"));

        mockMvc.perform(post(CLAIMS + "/{id}/submit", claim).header("Authorization", bearer(tenant, admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("AWAITING_ANALYSIS"))
                .andExpect(jsonPath("$.data.warnings").isEmpty());
    }

    @Test
    void claimOnExpiredPolicyIsHardRejected() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        long admin = roleIdByCode("TENANT_ADMIN");
        long customer = insertCustomer(tenant, "CUST-1");
        long policy = setupPolicy(tenant, admin, customer);

        // Incident after the policy's effective_to (2025-03-31).
        long claim = createDraft(tenant, admin,
                claimBody(customer, policy, "2025-08-01", 50000, "MH-12-AB-1234"));

        mockMvc.perform(post(CLAIMS + "/{id}/submit", claim).header("Authorization", bearer(tenant, admin)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("POLICY_NOT_ACTIVE_ON_LOSS_DATE")));
    }

    @Test
    void overLimitClaimSubmitsWithSoftWarning() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        long admin = roleIdByCode("TENANT_ADMIN");
        long customer = insertCustomer(tenant, "CUST-1");
        long policy = setupPolicy(tenant, admin, customer);

        // Amount 700000 > sum insured 650000 -> soft signal, NOT a rejection.
        long claim = createDraft(tenant, admin,
                claimBody(customer, policy, "2024-06-01", 700000, "MH-12-AB-1234"));

        mockMvc.perform(post(CLAIMS + "/{id}/submit", claim).header("Authorization", bearer(tenant, admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("AWAITING_ANALYSIS"))
                .andExpect(jsonPath("$.data.warnings[0]").value("POTENTIAL_OVER_LIMIT_CLAIM"));
    }

    @Test
    void vehicleMismatchIsHardRejected() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        long admin = roleIdByCode("TENANT_ADMIN");
        long customer = insertCustomer(tenant, "CUST-1");
        long policy = setupPolicy(tenant, admin, customer);

        // A different vehicle than the one on the policy.
        long claim = createDraft(tenant, admin,
                claimBody(customer, policy, "2024-06-01", 50000, "XX-99-ZZ-0000"));

        mockMvc.perform(post(CLAIMS + "/{id}/submit", claim).header("Authorization", bearer(tenant, admin)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("VEHICLE_NOT_COVERED_BY_POLICY")));
    }
}
