package com.niyotechnologies.claimlens.coverage;

import com.niyotechnologies.claimlens.product.enums.ProductVersionStatus;
import com.niyotechnologies.claimlens.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Policy-intelligence RAG (default stub embeddings + extractive answers — no API key). Ingest wording
 * for a product version, then ask a question and get a grounded, cited answer. Retrieval is
 * @TenantId-scoped and filtered to the version, so another tenant can't query it.
 */
class CoverageRagIntegrationTest extends AbstractIntegrationTest {

    // Two distinct, long paragraphs (each > the ~900-char chunk target) so chunking produces separate
    // chunks and retrieval ranking is genuinely exercised.
    private static final String WINDSHIELD =
            "Windshield and glass cover. The policy covers repair or replacement of the vehicle "
                    + "windshield and window glass damaged in an accident, up to the insured glass "
                    + "limit stated in the schedule. Windshield claims do not affect the no-claim bonus "
                    + "and no depreciation is applied to the glass component. Chipped or cracked "
                    + "windshield glass is repaired where it is safe to do so, otherwise the windshield "
                    + "is replaced using original equipment glass approved by the manufacturer for the "
                    + "vehicle model. Cover extends to the rear windshield, side window glass, and a "
                    + "factory-fitted glass sunroof. Labour charges for fitting and removal of the "
                    + "windshield are payable in full. A separate glass excess may apply for repeat "
                    + "windshield claims within the same policy year, and the surveyor may inspect the "
                    + "damaged windshield glass before authorising replacement to confirm the loss.";
    private static final String THEFT =
            "Theft and burglary cover. The policy covers loss of the insured vehicle due to theft, "
                    + "housebreaking or burglary. The claimant must file a police First Information "
                    + "Report without delay and surrender both keys of the stolen vehicle to the "
                    + "insurer. A claim for theft is settled at the insured declared value after "
                    + "deduction of any applicable compulsory and voluntary excess amounts, provided "
                    + "the vehicle was locked and reasonably secured at the time of the loss. Where an "
                    + "anti-theft device was fitted and certified, an additional discount applies. The "
                    + "insurer may require a non-traceable certificate from the police before settling "
                    + "a total-loss theft claim, and on settlement the ownership and salvage of the "
                    + "recovered vehicle transfer to the insurer as a condition of the theft payout.";
    private static final String POLICY_TEXT = WINDSHIELD + "\n\n" + THEFT;

    /** [productId, versionId] for a fresh active product version in the tenant. */
    private long[] activeVersion(long tenant) {
        long product = insertProduct(tenant, "PVT_CAR_RAG");
        long version = insertProductVersion(tenant, product, 1, ProductVersionStatus.ACTIVE);
        return new long[] {product, version};
    }

    @Test
    void ingestThenAskReturnsGroundedCitedAnswer() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        long admin = roleIdByCode("TENANT_ADMIN");
        long[] pv = activeVersion(tenant);
        long productId = pv[0];
        long versionId = pv[1];
        String auth = "Bearer " + tokenFor(1L, tenant, admin);

        mockMvc.perform(post("/api/v1/products/{p}/versions/{v}/knowledge", productId, versionId)
                        .header("Authorization", auth)
                        .contentType("application/json")
                        .content("{\"text\":" + jsonString(POLICY_TEXT) + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chunkCount", greaterThan(1)));

        mockMvc.perform(post("/api/v1/coverage/ask")
                        .header("Authorization", auth)
                        .contentType("application/json")
                        .content("{\"insuranceProductVersionId\":" + versionId
                                + ",\"question\":\"Is windshield glass damage covered?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.citations[0].snippet", containsStringIgnoringCase("windshield")))
                .andExpect(jsonPath("$.data.answer", containsStringIgnoringCase("windshield")));
    }

    @Test
    void reIngestSucceedsAfterAnswersHaveCitedTheChunks() throws Exception {
        // Regression: a prior answer creates coverage_answer_citation rows referencing policy_chunk.
        // Re-ingest deletes those chunks — which must first drop the citations, else an FK violation.
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        long admin = roleIdByCode("TENANT_ADMIN");
        long[] pv = activeVersion(tenant);
        String auth = "Bearer " + tokenFor(1L, tenant, admin);
        String knowledge = "{\"text\":" + jsonString(POLICY_TEXT) + "}";

        mockMvc.perform(post("/api/v1/products/{p}/versions/{v}/knowledge", pv[0], pv[1])
                        .header("Authorization", auth).contentType("application/json").content(knowledge))
                .andExpect(status().isOk());
        // Ask -> persists a citation pointing at a chunk.
        mockMvc.perform(post("/api/v1/coverage/ask").header("Authorization", auth)
                        .contentType("application/json")
                        .content("{\"insuranceProductVersionId\":" + pv[1] + ",\"question\":\"theft?\"}"))
                .andExpect(status().isOk());
        // Re-ingest must NOT 500 on the FK.
        mockMvc.perform(post("/api/v1/products/{p}/versions/{v}/knowledge", pv[0], pv[1])
                        .header("Authorization", auth).contentType("application/json").content(knowledge))
                .andExpect(status().isOk());
    }

    @Test
    void retrievalIsTenantScoped() throws Exception {
        long tenantA = insertCompany("Alpha", "ALPHA", "alpha");
        long tenantB = insertCompany("Beta", "BETA", "beta");
        long admin = roleIdByCode("TENANT_ADMIN");
        long[] pv = activeVersion(tenantA);
        long productId = pv[0];
        long versionId = pv[1];

        mockMvc.perform(post("/api/v1/products/{p}/versions/{v}/knowledge", productId, versionId)
                        .header("Authorization", "Bearer " + tokenFor(1L, tenantA, admin))
                        .contentType("application/json")
                        .content("{\"text\":" + jsonString(POLICY_TEXT) + "}"))
                .andExpect(status().isOk());

        // Tenant B asking about tenant A's version: the version isn't visible to B -> 404, not data.
        mockMvc.perform(post("/api/v1/coverage/ask")
                        .header("Authorization", "Bearer " + tokenFor(2L, tenantB, admin))
                        .contentType("application/json")
                        .content("{\"insuranceProductVersionId\":" + versionId
                                + ",\"question\":\"Is theft covered?\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void ingestRequiresCoverageWrite() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        long investigator = roleIdByCode("INVESTIGATOR"); // has COVERAGE_READ, not COVERAGE_WRITE
        long[] pv = activeVersion(tenant);
        long productId = pv[0];
        long versionId = pv[1];

        mockMvc.perform(post("/api/v1/products/{p}/versions/{v}/knowledge", productId, versionId)
                        .header("Authorization", "Bearer " + tokenFor(1L, tenant, investigator))
                        .contentType("application/json")
                        .content("{\"text\":\"Some wording\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void knowledgeStatusReportsChunkCount() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        long admin = roleIdByCode("TENANT_ADMIN");
        long[] pv = activeVersion(tenant);
        long productId = pv[0];
        long versionId = pv[1];
        String auth = "Bearer " + tokenFor(1L, tenant, admin);

        mockMvc.perform(post("/api/v1/products/{p}/versions/{v}/knowledge", productId, versionId)
                        .header("Authorization", auth).contentType("application/json")
                        .content("{\"text\":" + jsonString(POLICY_TEXT) + "}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/products/{p}/versions/{v}/knowledge", productId, versionId)
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chunkCount", greaterThan(0)));
    }

    private static String jsonString(String value) {
        return "\"" + value.replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }
}
