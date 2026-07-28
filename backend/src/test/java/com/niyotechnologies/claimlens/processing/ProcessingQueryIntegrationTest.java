package com.niyotechnologies.claimlens.processing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.niyotechnologies.claimlens.product.enums.ProductVersionStatus;
import com.niyotechnologies.claimlens.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The read side of the pipeline (GET /claims/{id}/processing): after submit + pipeline, it returns the
 * processing state and fraud score. Tenant-scoped like the rest (cross-tenant claim -> 404).
 */
class ProcessingQueryIntegrationTest extends AbstractIntegrationTest {

    private final ObjectMapper om = new ObjectMapper();

    private long submitAndProcessClaim(long tenant, long adminRole) throws Exception {
        String auth = "Bearer " + tokenFor(1L, tenant, adminRole);
        long customer = insertCustomer(tenant, "CUST-P1");
        long product = insertProduct(tenant, "PVT_CAR_PROC");
        insertProductVersion(tenant, product, 1, ProductVersionStatus.ACTIVE);
        String policyBody = "{\"policyNumber\":\"MOT-P1\",\"customerId\":" + customer
                + ",\"insuranceProductId\":" + product
                + ",\"effectiveFrom\":\"2024-04-01\",\"effectiveTo\":\"2025-03-31\",\"sumInsured\":650000,"
                + "\"vehicle\":{\"registrationNumber\":\"MH 12 AB 4321\",\"make\":\"Maruti\",\"model\":\"Swift\"}}";
        MvcResult pr = mockMvc.perform(post("/api/v1/policies").header("Authorization", auth)
                        .contentType("application/json").content(policyBody))
                .andExpect(status().isCreated()).andReturn();
        long policy = om.readTree(pr.getResponse().getContentAsString()).get("data").get("id").asLong();

        MvcResult dr = mockMvc.perform(post("/api/v1/claims").header("Authorization", auth)
                        .contentType("application/json")
                        .content("{\"customerId\":" + customer + ",\"insurancePolicyId\":" + policy
                                + ",\"incidentDate\":\"2024-06-01\",\"claimAmount\":50000,"
                                + "\"vehicleRegistrationNumber\":\"MH-12-AB-4321\"}"))
                .andExpect(status().isCreated()).andReturn();
        long claimId = om.readTree(dr.getResponse().getContentAsString()).get("data").get("id").asLong();

        mockMvc.perform(post("/api/v1/claims/{id}/submit", claimId).header("Authorization", auth))
                .andExpect(status().isOk());
        drivePipeline();
        return claimId;
    }

    @Test
    void processingViewReturnsStateAndFraudScore() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        long admin = roleIdByCode("TENANT_ADMIN");
        long claimId = submitAndProcessClaim(tenant, admin);

        mockMvc.perform(get("/api/v1/claims/{id}/processing", claimId)
                        .header("Authorization", "Bearer " + tokenFor(1L, tenant, admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state.ocrStatus").value("COMPLETE"))
                .andExpect(jsonPath("$.data.fraud.riskLevel").exists());
    }

    @Test
    void fraudBlockIsHiddenFromRolesWithoutFraudRead() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        long admin = roleIdByCode("TENANT_ADMIN");
        long claimId = submitAndProcessClaim(tenant, admin);

        // CUSTOMER_SUPPORT holds CLAIM_READ but not FRAUD_READ. It may see processing STATUS but the
        // fraud score/risk/explanation — a restricted assessment — must be withheld. Before the fix
        // FRAUD_READ was enforced nowhere and the score leaked to every CLAIM_READ role.
        mockMvc.perform(get("/api/v1/claims/{id}/processing", claimId)
                        .header("Authorization", "Bearer " + tokenFor(1L, tenant, roleIdByCode("CUSTOMER_SUPPORT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state.ocrStatus").value("COMPLETE"))  // status still visible
                .andExpect(jsonPath("$.data.fraud").doesNotExist());              // fraud block withheld

        // An investigation role keeps full visibility.
        mockMvc.perform(get("/api/v1/claims/{id}/processing", claimId)
                        .header("Authorization", "Bearer " + tokenFor(1L, tenant, roleIdByCode("INVESTIGATOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fraud.riskLevel").exists());
    }

    @Test
    void processingViewIsTenantIsolated() throws Exception {
        long tenantA = insertCompany("Alpha", "ALPHA", "alpha");
        long tenantB = insertCompany("Beta", "BETA", "beta");
        long admin = roleIdByCode("TENANT_ADMIN");
        long claimId = submitAndProcessClaim(tenantA, admin);

        mockMvc.perform(get("/api/v1/claims/{id}/processing", claimId)
                        .header("Authorization", "Bearer " + tokenFor(1L, tenantB, admin)))
                .andExpect(status().isNotFound());
    }
}
