package com.niyotechnologies.claimlens.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.niyotechnologies.claimlens.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Product lifecycle: an admin can rename a product and retire it; the change is scoped to the tenant
 * and gated behind PRODUCT_WRITE (proven by the tenant-isolation + RBAC suites elsewhere).
 */
class ProductIntegrationTest extends AbstractIntegrationTest {

    private static final String PRODUCTS = "/api/v1/products";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void updateProductRenamesAndRetiresIt() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        long admin = roleIdByCode("TENANT_ADMIN");
        long product = insertProduct(tenant, "PVT_CAR_PREMIUM");

        MvcResult updated = mockMvc.perform(put(PRODUCTS + "/{id}", product)
                        .header("Authorization", "Bearer " + tokenFor(1L, tenant, admin))
                        .contentType("application/json")
                        .content("{\"name\":\"Private Car Elite\",\"description\":\"Top tier\","
                                + "\"status\":\"RETIRED\"}"))
                .andExpect(status().isOk())
                .andReturn();

        var data = objectMapper.readTree(updated.getResponse().getContentAsString()).get("data");
        assertEquals("Private Car Elite", data.get("name").asText());
        assertEquals("RETIRED", data.get("status").asText());
    }
}
