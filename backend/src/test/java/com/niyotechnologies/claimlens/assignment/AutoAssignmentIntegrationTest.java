package com.niyotechnologies.claimlens.assignment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.niyotechnologies.claimlens.assignment.repository.ClaimAssignmentRepository;
import com.niyotechnologies.claimlens.product.enums.ProductVersionStatus;
import com.niyotechnologies.claimlens.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The assignment engine's V1 strategy: auto-assign the least-loaded active investigator.
 */
class AutoAssignmentIntegrationTest extends AbstractIntegrationTest {

    private final ObjectMapper om = new ObjectMapper();

    @Autowired
    private ClaimAssignmentRepository assignmentRepository;

    private String auth(long tenant, long role) {
        return "Bearer " + tokenFor(1L, tenant, role);
    }

    private long policyId;
    private long customerId;

    private void setupPolicy(long tenant, long admin) throws Exception {
        customerId = insertCustomer(tenant, "CUST-1");
        long product = insertProduct(tenant, "PVT_CAR_PREMIUM");
        insertProductVersion(tenant, product, 1, ProductVersionStatus.ACTIVE);
        String body = "{\"policyNumber\":\"MOT-1\",\"customerId\":" + customerId
                + ",\"insuranceProductId\":" + product
                + ",\"effectiveFrom\":\"2024-04-01\",\"effectiveTo\":\"2025-03-31\",\"sumInsured\":650000,"
                + "\"vehicle\":{\"registrationNumber\":\"MH 12 AB 1234\",\"make\":\"Maruti\",\"model\":\"Swift\"}}";
        MvcResult r = mockMvc.perform(post("/api/v1/policies").header("Authorization", auth(tenant, admin))
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated()).andReturn();
        policyId = om.readTree(r.getResponse().getContentAsString()).get("data").get("id").asLong();
    }

    /** Creates a claim, submits it, and drives the pipeline so it reaches AWAITING_ASSIGNMENT. */
    private long claimReadyForAssignment(long tenant, long admin, String incidentDate) throws Exception {
        MvcResult dr = mockMvc.perform(post("/api/v1/claims").header("Authorization", auth(tenant, admin))
                        .contentType("application/json")
                        .content("{\"customerId\":" + customerId + ",\"insurancePolicyId\":" + policyId
                                + ",\"incidentDate\":\"" + incidentDate + "\",\"claimAmount\":50000,"
                                + "\"vehicleRegistrationNumber\":\"MH-12-AB-1234\"}"))
                .andExpect(status().isCreated()).andReturn();
        long claimId = om.readTree(dr.getResponse().getContentAsString()).get("data").get("id").asLong();
        mockMvc.perform(post("/api/v1/claims/{id}/submit", claimId).header("Authorization", auth(tenant, admin)))
                .andExpect(status().isOk());
        drivePipeline();
        return claimId;
    }

    @Test
    void autoAssignPicksTheLeastLoadedInvestigator() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        long admin = roleIdByCode("TENANT_ADMIN");
        long investigatorRole = roleIdByCode("INVESTIGATOR");
        long inv1 = insertUser(tenant, "inv1@alpha.test", "h", investigatorRole);
        long inv2 = insertUser(tenant, "inv2@alpha.test", "h", investigatorRole);
        setupPolicy(tenant, admin);

        // Load inv1 by manually assigning it the first claim.
        long claim1 = claimReadyForAssignment(tenant, admin, "2024-06-01");
        mockMvc.perform(post("/api/v1/claims/{id}/assign", claim1).header("Authorization", auth(tenant, admin))
                        .contentType("application/json").content("{\"investigatorUserId\":" + inv1 + "}"))
                .andExpect(status().isOk());

        // Auto-assign a second claim -> should go to inv2 (load 0), not inv1 (load 1).
        long claim2 = claimReadyForAssignment(tenant, admin, "2024-07-01");
        mockMvc.perform(post("/api/v1/claims/{id}/auto-assign", claim2).header("Authorization", auth(tenant, admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UNDER_INVESTIGATION"));

        long assignedTo = inTenant(tenant,
                () -> assignmentRepository.findAllByClaimId(claim2).get(0).getInvestigatorUserId());
        assertEquals(inv2, assignedTo, "auto-assign must pick the least-loaded investigator");
    }
}
