package com.niyotechnologies.claimlens.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.niyotechnologies.claimlens.product.enums.ProductVersionStatus;
import com.niyotechnologies.claimlens.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Phase 10: audit trail, in-app notification on assignment, and the analytics dashboard. */
class Phase10IntegrationTest extends AbstractIntegrationTest {

    private final ObjectMapper om = new ObjectMapper();

    private String auth(long tenant, long role) {
        return "Bearer " + tokenFor(1L, tenant, role);
    }

    /** create -> submit -> pipeline -> assign(investigator) -> decide(approve); returns claimId. */
    private long runClaimToApproved(long tenant, long admin, long investigatorId) throws Exception {
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
                                + ",\"incidentDate\":\"2024-06-01\",\"claimAmount\":50000,"
                                + "\"vehicleRegistrationNumber\":\"MH-12-AB-1234\"}"))
                .andExpect(status().isCreated()).andReturn();
        long claimId = om.readTree(dr.getResponse().getContentAsString()).get("data").get("id").asLong();

        mockMvc.perform(post("/api/v1/claims/{id}/submit", claimId).header("Authorization", auth(tenant, admin)))
                .andExpect(status().isOk());
        drivePipeline();
        mockMvc.perform(post("/api/v1/claims/{id}/assign", claimId).header("Authorization", auth(tenant, admin))
                        .contentType("application/json").content("{\"investigatorUserId\":" + investigatorId + "}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/claims/{id}/decision", claimId).header("Authorization", auth(tenant, admin))
                        .contentType("application/json").content("{\"decision\":\"APPROVE\"}"))
                .andExpect(status().isOk());
        return claimId;
    }

    @Test
    void auditTrailRecordsClaimActions() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        long admin = roleIdByCode("TENANT_ADMIN");
        long investigator = insertUser(tenant, "inv@alpha.test", "h", roleIdByCode("INVESTIGATOR"));
        long claimId = runClaimToApproved(tenant, admin, investigator);

        mockMvc.perform(get("/api/v1/claims/{id}/audit", claimId).header("Authorization", auth(tenant, admin)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("CLAIM_SUBMITTED")))
                .andExpect(content().string(containsString("CLAIM_ASSIGNED")))
                .andExpect(content().string(containsString("CLAIM_DECIDED")));
    }

    @Test
    void investigatorIsNotifiedOnAssignment() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        long admin = roleIdByCode("TENANT_ADMIN");
        long investigatorRole = roleIdByCode("INVESTIGATOR");
        long investigator = insertUser(tenant, "inv@alpha.test", "h", investigatorRole);
        runClaimToApproved(tenant, admin, investigator);

        // Fetch notifications AS the investigator (token sub = investigator user id).
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + tokenFor(investigator, tenant, investigatorRole)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("New claim assigned")));
    }

    @Test
    void dashboardAggregatesClaimsByStatus() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        long admin = roleIdByCode("TENANT_ADMIN");
        long investigator = insertUser(tenant, "inv@alpha.test", "h", roleIdByCode("INVESTIGATOR"));
        runClaimToApproved(tenant, admin, investigator);

        mockMvc.perform(get("/api/v1/analytics/dashboard").header("Authorization", auth(tenant, admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.claimsByStatus.APPROVED").value(1));
    }
}
