package com.niyotechnologies.claimlens.claim;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.niyotechnologies.claimlens.product.enums.ProductVersionStatus;
import com.niyotechnologies.claimlens.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The demoable thin slice, end-to-end: create draft -> submit -> upload a document -> assign to an
 * investigator -> decide (approve). OCR/analysis/fraud are stubbed (skipped) in the manual path.
 */
class ClaimWorkflowIntegrationTest extends AbstractIntegrationTest {

    private static final String CLAIMS = "/api/v1/claims";
    private final ObjectMapper om = new ObjectMapper();

    private String auth(long tenant, long role) {
        return "Bearer " + tokenFor(1L, tenant, role);
    }

    private long createPolicy(long tenant, long admin, long customerId) throws Exception {
        long product = insertProduct(tenant, "PVT_CAR_PREMIUM");
        insertProductVersion(tenant, product, 1, ProductVersionStatus.ACTIVE);
        String body = "{\"policyNumber\":\"MOT-1\",\"customerId\":" + customerId
                + ",\"insuranceProductId\":" + product
                + ",\"effectiveFrom\":\"2024-04-01\",\"effectiveTo\":\"2025-03-31\",\"sumInsured\":650000,"
                + "\"vehicle\":{\"registrationNumber\":\"MH 12 AB 1234\",\"make\":\"Maruti\",\"model\":\"Swift\"}}";
        MvcResult r = mockMvc.perform(post("/api/v1/policies").header("Authorization", auth(tenant, admin))
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated()).andReturn();
        return om.readTree(r.getResponse().getContentAsString()).get("data").get("id").asLong();
    }

    @Test
    void fullClaimLifecycleFromDraftToApproved() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        long admin = roleIdByCode("TENANT_ADMIN");
        long customer = insertCustomer(tenant, "CUST-1");
        long policy = createPolicy(tenant, admin, customer);
        long investigator = insertUser(tenant, "inv@alpha.test", "unused-hash",
                roleIdByCode("INVESTIGATOR"));

        // 1. Create draft.
        MvcResult draft = mockMvc.perform(post(CLAIMS).header("Authorization", auth(tenant, admin))
                        .contentType("application/json")
                        .content("{\"customerId\":" + customer + ",\"insurancePolicyId\":" + policy
                                + ",\"incidentDate\":\"2024-06-01\",\"claimAmount\":50000,"
                                + "\"vehicleRegistrationNumber\":\"MH-12-AB-1234\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn();
        long claimId = om.readTree(draft.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 2. Submit -> enters background processing.
        mockMvc.perform(post(CLAIMS + "/{id}/submit", claimId).header("Authorization", auth(tenant, admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("AWAITING_ANALYSIS"));

        // 3. Upload a document (multipart).
        MockMultipartFile file = new MockMultipartFile(
                "file", "accident.jpg", "image/jpeg", "fake-image-bytes".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart(CLAIMS + "/{id}/documents", claimId)
                        .file(file)
                        .param("documentType", "ACCIDENT_PHOTO")
                        .header("Authorization", auth(tenant, admin)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.fileName").value("accident.jpg"));

        // 3b. Run the async pipeline (OCR -> analysis -> fraud) -> AWAITING_ASSIGNMENT.
        drivePipeline();
        mockMvc.perform(get(CLAIMS + "/{id}", claimId).header("Authorization", auth(tenant, admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("AWAITING_ASSIGNMENT"));

        // 4. Assign to the investigator.
        mockMvc.perform(post(CLAIMS + "/{id}/assign", claimId).header("Authorization", auth(tenant, admin))
                        .contentType("application/json")
                        .content("{\"investigatorUserId\":" + investigator + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UNDER_INVESTIGATION"));

        // 5. Decide: approve.
        mockMvc.perform(post(CLAIMS + "/{id}/decision", claimId).header("Authorization", auth(tenant, admin))
                        .contentType("application/json")
                        .content("{\"decision\":\"APPROVE\",\"reason\":\"Documents verified\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    void cannotAssignAClaimToaNonInvestigator() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        long admin = roleIdByCode("TENANT_ADMIN");
        long customer = insertCustomer(tenant, "CUST-1");
        long policy = createPolicy(tenant, admin, customer);
        // An auditor exists in the tenant but holds no CLAIM_INVESTIGATE permission.
        long auditor = insertUser(tenant, "aud@alpha.test", "unused-hash", roleIdByCode("AUDITOR"));

        MvcResult draft = mockMvc.perform(post(CLAIMS).header("Authorization", auth(tenant, admin))
                        .contentType("application/json")
                        .content("{\"customerId\":" + customer + ",\"insurancePolicyId\":" + policy
                                + ",\"incidentDate\":\"2024-06-01\",\"claimAmount\":50000,"
                                + "\"vehicleRegistrationNumber\":\"MH-12-AB-1234\"}"))
                .andExpect(status().isCreated()).andReturn();
        long claimId = om.readTree(draft.getResponse().getContentAsString()).get("data").get("id").asLong();
        mockMvc.perform(post(CLAIMS + "/{id}/submit", claimId).header("Authorization", auth(tenant, admin)))
                .andExpect(status().isOk());
        drivePipeline();

        // Assigning to a user who cannot investigate must be rejected — otherwise the claim would be
        // stranded in UNDER_INVESTIGATION with an assignee who has no way to act on it.
        mockMvc.perform(post(CLAIMS + "/{id}/assign", claimId).header("Authorization", auth(tenant, admin))
                        .contentType("application/json")
                        .content("{\"investigatorUserId\":" + auditor + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOT_AN_INVESTIGATOR"));

        // The claim must remain assignable, not silently transitioned.
        mockMvc.perform(get(CLAIMS + "/{id}", claimId).header("Authorization", auth(tenant, admin)))
                .andExpect(jsonPath("$.data.status").value("AWAITING_ASSIGNMENT"));
    }

    @Test
    void malformedRequestBodyIsBadRequestNotServerError() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        long admin = roleIdByCode("TENANT_ADMIN");

        // An unknown enum constant can't deserialize -> HttpMessageNotReadableException. Without its
        // handler this escaped as a 500; a malformed body is the client's 400.
        mockMvc.perform(post(CLAIMS + "/{id}/decision", 1).header("Authorization", auth(tenant, admin))
                        .contentType("application/json")
                        .content("{\"decision\":\"NONSENSE\",\"reason\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST_BODY"));
    }

    @Test
    void cannotDecideAClaimThatIsNotUnderInvestigation() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        long admin = roleIdByCode("TENANT_ADMIN");
        long customer = insertCustomer(tenant, "CUST-1");
        long policy = createPolicy(tenant, admin, customer);

        MvcResult draft = mockMvc.perform(post(CLAIMS).header("Authorization", auth(tenant, admin))
                        .contentType("application/json")
                        .content("{\"customerId\":" + customer + ",\"insurancePolicyId\":" + policy
                                + ",\"incidentDate\":\"2024-06-01\",\"claimAmount\":50000,"
                                + "\"vehicleRegistrationNumber\":\"MH-12-AB-1234\"}"))
                .andExpect(status().isCreated()).andReturn();
        long claimId = om.readTree(draft.getResponse().getContentAsString()).get("data").get("id").asLong();

        // Still DRAFT -> deciding is a business-rule violation (400).
        mockMvc.perform(post(CLAIMS + "/{id}/decision", claimId).header("Authorization", auth(tenant, admin))
                        .contentType("application/json")
                        .content("{\"decision\":\"APPROVE\"}"))
                .andExpect(status().isBadRequest());
    }
}
