package com.niyotechnologies.claimlens.document;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.niyotechnologies.claimlens.product.enums.ProductVersionStatus;
import com.niyotechnologies.claimlens.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Document versioning: re-uploading a document appends a new version, preserves the old bytes, and
 * repoints the live version — so a claim keeps the full paper trail (e.g. the original RC and the
 * corrected RC the customer sent later).
 */
class DocumentVersioningIntegrationTest extends AbstractIntegrationTest {

    private static final String CLAIMS = "/api/v1/claims";
    private final ObjectMapper om = new ObjectMapper();

    private String auth(long tenant, long role) {
        return "Bearer " + tokenFor(1L, tenant, role);
    }

    private long createClaimWithPolicy(long tenant, long admin, long customer) throws Exception {
        long product = insertProduct(tenant, "PVT_CAR_PREMIUM");
        insertProductVersion(tenant, product, 1, ProductVersionStatus.ACTIVE);
        String policyBody = "{\"policyNumber\":\"MOT-1\",\"customerId\":" + customer
                + ",\"insuranceProductId\":" + product
                + ",\"effectiveFrom\":\"2024-04-01\",\"effectiveTo\":\"2025-03-31\",\"sumInsured\":650000,"
                + "\"vehicle\":{\"registrationNumber\":\"MH 12 AB 1234\",\"make\":\"Maruti\",\"model\":\"Swift\"}}";
        MvcResult p = mockMvc.perform(post("/api/v1/policies").header("Authorization", auth(tenant, admin))
                        .contentType("application/json").content(policyBody))
                .andExpect(status().isCreated()).andReturn();
        long policy = om.readTree(p.getResponse().getContentAsString()).get("data").get("id").asLong();

        MvcResult d = mockMvc.perform(post(CLAIMS).header("Authorization", auth(tenant, admin))
                        .contentType("application/json")
                        .content("{\"customerId\":" + customer + ",\"insurancePolicyId\":" + policy
                                + ",\"incidentDate\":\"2024-06-01\",\"claimAmount\":50000,"
                                + "\"vehicleRegistrationNumber\":\"MH-12-AB-1234\"}"))
                .andExpect(status().isCreated()).andReturn();
        return om.readTree(d.getResponse().getContentAsString()).get("data").get("id").asLong();
    }

    private MockMultipartFile file(String name, String body) {
        return new MockMultipartFile("file", name, "image/jpeg", body.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void reuploadAppendsAVersionAndKeepsHistory() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        long admin = roleIdByCode("TENANT_ADMIN");
        long customer = insertCustomer(tenant, "CUST-1");
        long claim = createClaimWithPolicy(tenant, admin, customer);

        // Upload the original RC -> document with version 1.
        MvcResult up = mockMvc.perform(multipart(CLAIMS + "/{id}/documents", claim)
                        .file(file("rc-v1.jpg", "original-rc-bytes"))
                        .param("documentType", "RC_BOOK")
                        .header("Authorization", auth(tenant, admin)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.fileName").value("rc-v1.jpg"))
                .andReturn();
        long documentId = om.readTree(up.getResponse().getContentAsString()).get("data").get("id").asLong();

        // Re-upload a corrected RC -> version 2, and the document's current file switches.
        mockMvc.perform(multipart(CLAIMS + "/{cid}/documents/{did}/versions", claim, documentId)
                        .file(file("rc-v2.jpg", "corrected-rc-bytes"))
                        .header("Authorization", auth(tenant, admin)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.fileName").value("rc-v2.jpg"));

        // Two versions, newest first, v2 current.
        MvcResult versions = mockMvc.perform(get(CLAIMS + "/{cid}/documents/{did}/versions", claim, documentId)
                        .header("Authorization", auth(tenant, admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andReturn();
        JsonNode arr = om.readTree(versions.getResponse().getContentAsString()).get("data");
        assertEquals(2, arr.get(0).get("versionNumber").asInt());
        assertEquals(true, arr.get(0).get("current").asBoolean());
        assertEquals(1, arr.get(1).get("versionNumber").asInt());
        assertEquals(false, arr.get(1).get("current").asBoolean());

        // The original v1 bytes are still downloadable — history is preserved, not overwritten.
        long v1Id = arr.get(1).get("id").asLong();
        MvcResult v1 = mockMvc.perform(get(CLAIMS + "/{cid}/documents/{did}/versions/{vid}/download",
                        claim, documentId, v1Id).header("Authorization", auth(tenant, admin)))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals("original-rc-bytes", v1.getResponse().getContentAsString());

        // The document's default download now serves v2.
        MvcResult current = mockMvc.perform(get(CLAIMS + "/{cid}/documents/{did}/download", claim, documentId)
                        .header("Authorization", auth(tenant, admin)))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals("corrected-rc-bytes", current.getResponse().getContentAsString());
    }

    @Test
    void cannotVersionAnotherTenantsDocument() throws Exception {
        long tenantA = insertCompany("Alpha", "ALPHA", "alpha");
        long tenantB = insertCompany("Beta", "BETA", "beta");
        long admin = roleIdByCode("TENANT_ADMIN");
        long customerB = insertCustomer(tenantB, "CUST-B");
        long claimB = createClaimWithPolicy(tenantB, admin, customerB);

        MvcResult up = mockMvc.perform(multipart(CLAIMS + "/{id}/documents", claimB)
                        .file(file("b.jpg", "b-bytes"))
                        .param("documentType", "RC_BOOK")
                        .header("Authorization", auth(tenantB, admin)))
                .andExpect(status().isCreated()).andReturn();
        long docB = om.readTree(up.getResponse().getContentAsString()).get("data").get("id").asLong();

        // Tenant A cannot add a version to tenant B's document -> 404.
        mockMvc.perform(multipart(CLAIMS + "/{cid}/documents/{did}/versions", claimB, docB)
                        .file(file("x.jpg", "x-bytes"))
                        .header("Authorization", auth(tenantA, admin)))
                .andExpect(status().isNotFound());
    }
}
