package com.niyotechnologies.claimlens.ruleset;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.niyotechnologies.claimlens.fraud.repository.FraudScoreRepository;
import com.niyotechnologies.claimlens.product.enums.ProductVersionStatus;
import com.niyotechnologies.claimlens.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves fraud scoring is CONFIG-DRIVEN: an active fraud ruleset overrides the built-in defaults
 * (different weight/thresholds), and disabling a rule removes its contribution — all without code.
 */
class ConfigurableFraudIntegrationTest extends AbstractIntegrationTest {

    private final ObjectMapper om = new ObjectMapper();

    @Autowired
    private FraudScoreRepository fraudScoreRepository;

    private String auth(long tenant, long role) {
        return "Bearer " + tokenFor(1L, tenant, role);
    }

    /** Submits an over-limit motor claim (amount 700000 > sum insured 650000) and runs the pipeline. */
    private long submitOverLimitClaim(long tenant, long admin) throws Exception {
        long customer = insertCustomer(tenant, "CUST-1");
        long product = insertProduct(tenant, "PVT_CAR_PREMIUM");
        insertProductVersion(tenant, product, 1, ProductVersionStatus.ACTIVE);
        String policyBody = "{\"policyNumber\":\"MOT-1\",\"customerId\":" + customer
                + ",\"insuranceProductId\":" + product
                + ",\"effectiveFrom\":\"2024-04-01\",\"effectiveTo\":\"2025-03-31\",\"sumInsured\":650000,"
                + "\"vehicle\":{\"registrationNumber\":\"MH 12 AB 1234\",\"make\":\"Maruti\",\"model\":\"Swift\"}}";
        MvcResult pr = mockMvc.perform(post("/api/v1/policies").header("Authorization", auth(tenant, admin))
                        .contentType("application/json").content(policyBody))
                .andExpect(status().isCreated()).andReturn();
        long policy = om.readTree(pr.getResponse().getContentAsString()).get("data").get("id").asLong();

        MvcResult dr = mockMvc.perform(post("/api/v1/claims").header("Authorization", auth(tenant, admin))
                        .contentType("application/json")
                        .content("{\"customerId\":" + customer + ",\"insurancePolicyId\":" + policy
                                + ",\"incidentDate\":\"2024-08-01\",\"claimAmount\":700000,"
                                + "\"vehicleRegistrationNumber\":\"MH-12-AB-1234\"}"))
                .andExpect(status().isCreated()).andReturn();
        long claimId = om.readTree(dr.getResponse().getContentAsString()).get("data").get("id").asLong();

        mockMvc.perform(post("/api/v1/claims/{id}/submit", claimId).header("Authorization", auth(tenant, admin)))
                .andExpect(status().isOk());
        return claimId;
    }

    private long createAndActivateRuleset(long tenant, long admin, int weight, boolean enabled) throws Exception {
        String body = "{\"claimTypeCode\":\"MOTOR\",\"name\":\"Motor rules\",\"mediumThreshold\":5,"
                + "\"highThreshold\":8,\"rules\":[{\"code\":\"AMOUNT_OVER_SUM_INSURED\",\"weight\":"
                + weight + ",\"enabled\":" + enabled + "}]}";
        MvcResult r = mockMvc.perform(post("/api/v1/rulesets/fraud").header("Authorization", auth(tenant, admin))
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated()).andReturn();
        long rulesetId = om.readTree(r.getResponse().getContentAsString()).get("data").get("id").asLong();
        mockMvc.perform(post("/api/v1/rulesets/fraud/{id}/activate", rulesetId)
                        .header("Authorization", auth(tenant, admin)))
                .andExpect(status().isOk());
        return rulesetId;
    }

    @Test
    void activeRulesetOverridesDefaultWeights() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        long admin = roleIdByCode("TENANT_ADMIN");

        // Config: AMOUNT_OVER_SUM_INSURED weighted 10 (built-in default is 40).
        createAndActivateRuleset(tenant, admin, 10, true);

        long claimId = submitOverLimitClaim(tenant, admin);
        drivePipeline();

        var score = fraudScoreRepository.findFirstByClaimIdOrderByCreatedAtDesc(claimId).orElseThrow();
        assertEquals(10, score.getScore(), "config weight (10) must override the default (40)");
        assertEquals("HIGH", score.getRiskLevel(), "10 >= high threshold (8) -> HIGH");
    }

    @Test
    void disablingARuleRemovesItsContribution() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        long admin = roleIdByCode("TENANT_ADMIN");

        // Same rule, but disabled.
        createAndActivateRuleset(tenant, admin, 10, false);

        long claimId = submitOverLimitClaim(tenant, admin);
        drivePipeline();

        var score = fraudScoreRepository.findFirstByClaimIdOrderByCreatedAtDesc(claimId).orElseThrow();
        assertEquals(0, score.getScore(), "a disabled rule contributes nothing");
        assertEquals("LOW", score.getRiskLevel());
    }
}
