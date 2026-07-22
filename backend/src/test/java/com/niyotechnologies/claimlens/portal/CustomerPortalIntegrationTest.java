package com.niyotechnologies.claimlens.portal;

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
 * The customer portal introduces a SECOND security boundary, below the tenant: two customers share a
 * tenant, so @TenantId does not separate them. This suite is the gate for that boundary — every
 * portal read/write is scoped to the authenticated customer, and customer A must get a 404 (never a
 * 200, never a 403) for customer B's claim in the same tenant.
 */
class CustomerPortalIntegrationTest extends AbstractIntegrationTest {

    private static final String POLICIES = "/api/v1/policies";
    private static final String PORTAL = "/api/v1/portal";
    private final ObjectMapper om = new ObjectMapper();

    /** Create an ACTIVE-version product + a policy owned by the given customer, via the staff API. */
    private long policyForCustomer(long tenant, long admin, long customerId, String policyNumber)
            throws Exception {
        long product = insertProduct(tenant, "PVT_" + policyNumber);
        insertProductVersion(tenant, product, 1, ProductVersionStatus.ACTIVE);
        String body = "{\"policyNumber\":\"" + policyNumber + "\",\"customerId\":" + customerId
                + ",\"insuranceProductId\":" + product
                + ",\"effectiveFrom\":\"2024-04-01\",\"effectiveTo\":\"2025-03-31\",\"sumInsured\":650000,"
                + "\"vehicle\":{\"registrationNumber\":\"MH 12 AB 1234\",\"make\":\"Maruti\",\"model\":\"Swift\"}}";
        MvcResult r = mockMvc.perform(post(POLICIES).header("Authorization", "Bearer " + tokenFor(1L, tenant, admin))
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated()).andReturn();
        return om.readTree(r.getResponse().getContentAsString()).get("data").get("id").asLong();
    }

    private String fileClaimBody(long policyId) {
        return "{\"insurancePolicyId\":" + policyId + ",\"incidentDate\":\"2024-06-01\","
                + "\"claimAmount\":50000,\"vehicleRegistrationNumber\":\"MH-12-AB-1234\"}";
    }

    @Test
    void customerFilesUploadsAndSubmitsOwnClaimEndToEnd() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        long admin = roleIdByCode("TENANT_ADMIN");
        long customerRole = roleIdByCode("CUSTOMER");
        long custA = insertCustomer(tenant, "CUST-A");
        long policyA = policyForCustomer(tenant, admin, custA, "MOT-A");
        String token = customerTokenFor(1L, tenant, customerRole, custA);

        // File a claim against my own policy -> DRAFT.
        MvcResult filed = mockMvc.perform(post(PORTAL + "/claims").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(fileClaimBody(policyA)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn();
        long claimId = om.readTree(filed.getResponse().getContentAsString()).get("data").get("id").asLong();

        // Upload a supporting document.
        MockMultipartFile file = new MockMultipartFile(
                "file", "rc.jpg", "image/jpeg", "rc-bytes".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart(PORTAL + "/claims/{id}/documents", claimId)
                        .file(file).param("documentType", "RC_BOOK")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        // Submit -> enters background processing.
        mockMvc.perform(post(PORTAL + "/claims/{id}/submit", claimId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("AWAITING_ANALYSIS"));

        // My claims and my policies each show exactly one item.
        mockMvc.perform(get(PORTAL + "/claims").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
        mockMvc.perform(get(PORTAL + "/policies").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void customerUploadAnswersInfoRequestAndReopensTheClaim() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        long admin = roleIdByCode("TENANT_ADMIN");
        long customerRole = roleIdByCode("CUSTOMER");
        long custA = insertCustomer(tenant, "CUST-A");
        long policyA = policyForCustomer(tenant, admin, custA, "MOT-A");
        long investigator = insertUser(tenant, "inv@alpha.test", "h", roleIdByCode("INVESTIGATOR"));
        String token = customerTokenFor(1L, tenant, customerRole, custA);
        String adminAuth = "Bearer " + tokenFor(1L, tenant, admin);

        // Customer files + uploads + submits.
        MvcResult filed = mockMvc.perform(post(PORTAL + "/claims").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(fileClaimBody(policyA)))
                .andExpect(status().isCreated()).andReturn();
        long claimId = om.readTree(filed.getResponse().getContentAsString()).get("data").get("id").asLong();
        mockMvc.perform(multipart(PORTAL + "/claims/{id}/documents", claimId)
                        .file(new MockMultipartFile("file", "rc.jpg", "image/jpeg",
                                "rc".getBytes(StandardCharsets.UTF_8)))
                        .param("documentType", "RC_BOOK").header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());
        mockMvc.perform(post(PORTAL + "/claims/{id}/submit", claimId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Process + assign -> UNDER_INVESTIGATION.
        drivePipeline();
        mockMvc.perform(post("/api/v1/claims/{id}/assign", claimId).header("Authorization", adminAuth)
                        .contentType("application/json").content("{\"investigatorUserId\":" + investigator + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UNDER_INVESTIGATION"));

        // Investigator requests info -> WAITING_FOR_CUSTOMER.
        mockMvc.perform(post("/api/v1/claims/{id}/request-information", claimId).header("Authorization", adminAuth)
                        .contentType("application/json").content("{\"message\":\"Please upload your FIR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WAITING_FOR_CUSTOMER"));

        // Customer answers by uploading the requested document -> claim re-opens automatically.
        mockMvc.perform(multipart(PORTAL + "/claims/{id}/documents", claimId)
                        .file(new MockMultipartFile("file", "fir.pdf", "application/pdf",
                                "fir".getBytes(StandardCharsets.UTF_8)))
                        .param("documentType", "FIR").header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());
        mockMvc.perform(get(PORTAL + "/claims/{id}", claimId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UNDER_INVESTIGATION"));

        // The pipeline re-runs to a terminal state without error.
        drivePipeline();
    }

    @Test
    void customerCannotSeeAnotherCustomersClaimSameTenant() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        long admin = roleIdByCode("TENANT_ADMIN");
        long customerRole = roleIdByCode("CUSTOMER");
        long custA = insertCustomer(tenant, "CUST-A");
        long custB = insertCustomer(tenant, "CUST-B");
        long policyB = policyForCustomer(tenant, admin, custB, "MOT-B");

        // B files a claim.
        String tokenB = customerTokenFor(2L, tenant, customerRole, custB);
        MvcResult filed = mockMvc.perform(post(PORTAL + "/claims").header("Authorization", "Bearer " + tokenB)
                        .contentType("application/json").content(fileClaimBody(policyB)))
                .andExpect(status().isCreated()).andReturn();
        long claimB = om.readTree(filed.getResponse().getContentAsString()).get("data").get("id").asLong();

        // A — same tenant, different customer — must NOT see it. 404, not 200, not 403.
        String tokenA = customerTokenFor(1L, tenant, customerRole, custA);
        mockMvc.perform(get(PORTAL + "/claims/{id}", claimB).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
        // A cannot upload to or submit B's claim either.
        mockMvc.perform(post(PORTAL + "/claims/{id}/submit", claimB).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
        // A's own claim list is empty.
        mockMvc.perform(get(PORTAL + "/claims").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void customerCannotFileAgainstAnotherCustomersPolicy() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        long admin = roleIdByCode("TENANT_ADMIN");
        long customerRole = roleIdByCode("CUSTOMER");
        long custA = insertCustomer(tenant, "CUST-A");
        long custB = insertCustomer(tenant, "CUST-B");
        long policyB = policyForCustomer(tenant, admin, custB, "MOT-B");

        // A tries to file against B's policy -> 404 (policy not found for this customer).
        String tokenA = customerTokenFor(1L, tenant, customerRole, custA);
        mockMvc.perform(post(PORTAL + "/claims").header("Authorization", "Bearer " + tokenA)
                        .contentType("application/json").content(fileClaimBody(policyB)))
                .andExpect(status().isNotFound());
    }

    @Test
    void staffUserWithoutPortalPermissionIsForbidden() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        long admin = roleIdByCode("TENANT_ADMIN");

        // A staff token has no PORTAL_* authority -> @PreAuthorize denies at 403.
        mockMvc.perform(get(PORTAL + "/claims").header("Authorization", "Bearer " + tokenFor(1L, tenant, admin)))
                .andExpect(status().isForbidden());
    }
}
