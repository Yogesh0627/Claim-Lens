package com.niyotechnologies.claimlens.processing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.niyotechnologies.claimlens.processing.orchestrator.ProcessingOrchestrator;
import com.niyotechnologies.claimlens.processing.repository.ClaimProcessingStateRepository;
import com.niyotechnologies.claimlens.processing.repository.FraudJobRepository;
import com.niyotechnologies.claimlens.fraud.repository.FraudScoreRepository;
import com.niyotechnologies.claimlens.product.enums.ProductVersionStatus;
import com.niyotechnologies.claimlens.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The async pipeline's crown jewel: exactly one fraud job per claim, and the full run to
 * AWAITING_ASSIGNMENT with a fraud score.
 */
class ProcessingPipelineIntegrationTest extends AbstractIntegrationTest {

    private final ObjectMapper om = new ObjectMapper();

    @Autowired
    private FraudJobRepository fraudJobRepository;
    @Autowired
    private FraudScoreRepository fraudScoreRepository;
    @Autowired
    private ClaimProcessingStateRepository stateRepository;
    @Autowired
    private ProcessingOrchestrator orchestrator;

    private long submitClaim(long tenant, long admin) throws Exception {
        long customer = insertCustomer(tenant, "CUST-1");
        long product = insertProduct(tenant, "PVT_CAR_PREMIUM");
        insertProductVersion(tenant, product, 1, ProductVersionStatus.ACTIVE);
        String policyBody = "{\"policyNumber\":\"MOT-1\",\"customerId\":" + customer
                + ",\"insuranceProductId\":" + product
                + ",\"effectiveFrom\":\"2024-04-01\",\"effectiveTo\":\"2025-03-31\",\"sumInsured\":650000,"
                + "\"vehicle\":{\"registrationNumber\":\"MH 12 AB 1234\",\"make\":\"Maruti\",\"model\":\"Swift\"}}";
        MvcResult pr = mockMvc.perform(post("/api/v1/policies").header("Authorization", "Bearer " + tokenFor(1L, tenant, admin))
                        .contentType("application/json").content(policyBody))
                .andExpect(status().isCreated()).andReturn();
        long policy = om.readTree(pr.getResponse().getContentAsString()).get("data").get("id").asLong();

        MvcResult dr = mockMvc.perform(post("/api/v1/claims").header("Authorization", "Bearer " + tokenFor(1L, tenant, admin))
                        .contentType("application/json")
                        .content("{\"customerId\":" + customer + ",\"insurancePolicyId\":" + policy
                                + ",\"incidentDate\":\"2024-08-01\",\"claimAmount\":700000,"
                                + "\"vehicleRegistrationNumber\":\"MH-12-AB-1234\"}"))
                .andExpect(status().isCreated()).andReturn();
        long claimId = om.readTree(dr.getResponse().getContentAsString()).get("data").get("id").asLong();

        mockMvc.perform(post("/api/v1/claims/{id}/submit", claimId).header("Authorization", "Bearer " + tokenFor(1L, tenant, admin)))
                .andExpect(status().isOk());
        return claimId;
    }

    @Test
    void exactlyOneFraudJobIsQueuedWhenBothStagesComplete() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        long admin = roleIdByCode("TENANT_ADMIN");
        long claimId = submitClaim(tenant, admin);

        // Before both stages complete, no fraud job.
        ocrWorker.pollOnce();                 // OCR complete; analysis still pending -> gate does NOT fire
        assertEquals(0, fraudJobRepository.findAllByClaimId(claimId).size(),
                "no fraud job until both OCR and analysis are complete");

        analysisWorker.pollOnce();            // analysis complete -> gate fires -> exactly one fraud job
        assertEquals(1, fraudJobRepository.findAllByClaimId(claimId).size(),
                "exactly one fraud job after both stages complete");

        // Re-attempting the gate must not create a second fraud job (idempotent).
        orchestrator.tryQueueFraud(claimId, tenant);
        assertEquals(1, fraudJobRepository.findAllByClaimId(claimId).size(),
                "the gate is idempotent — still exactly one fraud job");
    }

    @Test
    void pipelineRunsToAwaitingAssignmentWithAFraudScore() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        long admin = roleIdByCode("TENANT_ADMIN");
        long claimId = submitClaim(tenant, admin);

        drivePipeline();  // OCR -> analysis -> fraud

        var score = fraudScoreRepository.findFirstByClaimIdOrderByCreatedAtDesc(claimId).orElseThrow();
        // Amount (700000) > sum insured (650000) => the over-limit rule fires => HIGH-ish risk.
        assertTrue(score.getScore() >= 40, "over-limit claim should score at least the over-limit weight");

        mockMvc.perform(post("/api/v1/claims/{id}/assign", claimId)
                        .header("Authorization", "Bearer " + tokenFor(1L, tenant, admin))
                        .contentType("application/json")
                        .content("{\"investigatorUserId\":" + insertUser(tenant, "inv@a.test", "h",
                                roleIdByCode("INVESTIGATOR")) + "}"))
                .andExpect(status().isOk());
    }
}
