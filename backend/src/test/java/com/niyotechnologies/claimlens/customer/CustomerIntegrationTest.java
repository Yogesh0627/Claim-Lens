package com.niyotechnologies.claimlens.customer;

import com.niyotechnologies.claimlens.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Customer CRUD under the same guarantees as the rest of the app: tenant isolation (@TenantId) and
 * permission checks (@PreAuthorize CUSTOMER_READ/WRITE).
 */
class CustomerIntegrationTest extends AbstractIntegrationTest {

    private static final String CUSTOMERS = "/api/v1/customers";
    private static final String BODY =
            "{\"customerNumber\":\"CUST-1\",\"firstName\":\"Test\",\"lastName\":\"Customer\","
                    + "\"email\":\"customer@x.test\",\"phone\":\"9999999999\"}";

    @Test
    void adminCreatesCustomerAndItIsTenantIsolated() throws Exception {
        long tenantA = insertCompany("Alpha", "ALPHA", "alpha");
        long tenantB = insertCompany("Beta", "BETA", "beta");
        long adminRole = roleIdByCode("TENANT_ADMIN");

        // Create as tenant A.
        mockMvc.perform(post(CUSTOMERS)
                        .header("Authorization", "Bearer " + tokenFor(1L, tenantA, adminRole))
                        .contentType("application/json")
                        .content(BODY))
                .andExpect(status().isCreated());

        // Tenant B cannot see A's customer.
        mockMvc.perform(get(CUSTOMERS)
                        .header("Authorization", "Bearer " + tokenFor(2L, tenantB, adminRole)))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .content().string(org.hamcrest.Matchers.not(
                                org.hamcrest.Matchers.containsString("CUST-1"))));

        // Tenant A sees it.
        mockMvc.perform(get(CUSTOMERS)
                        .header("Authorization", "Bearer " + tokenFor(1L, tenantA, adminRole)))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .content().string(org.hamcrest.Matchers.containsString("CUST-1")));
    }

    @Test
    void investigatorCannotWriteCustomers() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        long investigatorRole = roleIdByCode("INVESTIGATOR"); // no CUSTOMER_WRITE

        mockMvc.perform(post(CUSTOMERS)
                        .header("Authorization", "Bearer " + tokenFor(1L, tenant, investigatorRole))
                        .contentType("application/json")
                        .content(BODY))
                .andExpect(status().isForbidden());
    }
}
