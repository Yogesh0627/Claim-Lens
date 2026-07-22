package com.niyotechnologies.claimlens.investigation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.niyotechnologies.claimlens.product.enums.ProductVersionStatus;
import com.niyotechnologies.claimlens.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Investigation notes: an investigator records findings while a claim is under investigation.
 * Guarded by CLAIM_INVESTIGATE (RBAC) and only allowed once the claim is under investigation.
 */
class InvestigationNotesIntegrationTest extends AbstractIntegrationTest {

    private static final String NOTE_BODY =
            "{\"noteType\":\"SITE_VISIT\",\"note\":\"Visited garage; damage consistent with report\","
                    + "\"severity\":\"LOW\"}";
    private final ObjectMapper om = new ObjectMapper();

    private String auth(long tenant, long role) {
        return "Bearer " + tokenFor(1L, tenant, role);
    }

    /** Drives a claim all the way to UNDER_INVESTIGATION and returns its id. */
    private long claimUnderInvestigation(long tenant, long admin, long investigatorId) throws Exception {
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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UNDER_INVESTIGATION"));
        return claimId;
    }

    @Test
    void investigatorAddsAndListsNotes() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        long admin = roleIdByCode("TENANT_ADMIN");
        long investigator = insertUser(tenant, "inv@alpha.test", "h", roleIdByCode("INVESTIGATOR"));
        long claimId = claimUnderInvestigation(tenant, admin, investigator);

        String notes = "/api/v1/claims/" + claimId + "/investigation-notes";
        mockMvc.perform(post(notes).header("Authorization", auth(tenant, admin))
                        .contentType("application/json").content(NOTE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.noteType").value("SITE_VISIT"))
                .andExpect(jsonPath("$.data.createdBy").value(1));

        mockMvc.perform(get(notes).header("Authorization", auth(tenant, admin)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Visited garage")));
    }

    @Test
    void investigatorRequestsInformationMovesClaimToWaitingForCustomer() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        long admin = roleIdByCode("TENANT_ADMIN");
        long investigator = insertUser(tenant, "inv@alpha.test", "h", roleIdByCode("INVESTIGATOR"));
        long claimId = claimUnderInvestigation(tenant, admin, investigator);

        String url = "/api/v1/claims/" + claimId + "/request-information";
        mockMvc.perform(post(url).header("Authorization", auth(tenant, admin))
                        .contentType("application/json")
                        .content("{\"message\":\"Please upload a clearer photo of your RC book\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WAITING_FOR_CUSTOMER"));

        // It's no longer under investigation, so a second request is rejected.
        mockMvc.perform(post(url).header("Authorization", auth(tenant, admin))
                        .contentType("application/json").content("{\"message\":\"Anything else?\"}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void requestInformationRequiresInvestigatePermission() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        long admin = roleIdByCode("TENANT_ADMIN");
        long investigator = insertUser(tenant, "inv@alpha.test", "h", roleIdByCode("INVESTIGATOR"));
        long claimId = claimUnderInvestigation(tenant, admin, investigator);

        // CUSTOMER_SUPPORT has CLAIM_READ/WRITE/SUBMIT but not CLAIM_INVESTIGATE.
        mockMvc.perform(post("/api/v1/claims/" + claimId + "/request-information")
                        .header("Authorization", auth(tenant, roleIdByCode("CUSTOMER_SUPPORT")))
                        .contentType("application/json").content("{\"message\":\"need docs\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void roleWithoutInvestigatePermissionIsForbidden() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        long admin = roleIdByCode("TENANT_ADMIN");
        long investigator = insertUser(tenant, "inv@alpha.test", "h", roleIdByCode("INVESTIGATOR"));
        long claimId = claimUnderInvestigation(tenant, admin, investigator);

        // CUSTOMER_SUPPORT has CLAIM_READ/WRITE/SUBMIT but not CLAIM_INVESTIGATE.
        mockMvc.perform(post("/api/v1/claims/" + claimId + "/investigation-notes")
                        .header("Authorization", auth(tenant, roleIdByCode("CUSTOMER_SUPPORT")))
                        .contentType("application/json").content(NOTE_BODY))
                .andExpect(status().isForbidden());
    }
}
