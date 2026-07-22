package com.niyotechnologies.claimlens.user;

import com.niyotechnologies.claimlens.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The user directory (GET /users) powers the investigator picker on claim assignment: tenant-scoped
 * by @TenantId, gated by USER_READ, optionally filtered by role code.
 */
class UserListIntegrationTest extends AbstractIntegrationTest {

    private static final String USERS = "/api/v1/users";

    @Test
    void adminListsUsersAndFiltersByRole() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        long adminRole = roleIdByCode("TENANT_ADMIN");
        long investigatorRole = roleIdByCode("INVESTIGATOR");
        long supportRole = roleIdByCode("CUSTOMER_SUPPORT");

        insertUser(tenant, "inv1@alpha.test", "h", investigatorRole);
        insertUser(tenant, "inv2@alpha.test", "h", investigatorRole);
        insertUser(tenant, "support@alpha.test", "h", supportRole);

        // Full directory: all three appear.
        mockMvc.perform(get(USERS).header("Authorization", "Bearer " + tokenFor(1L, tenant, adminRole)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("inv1@alpha.test")))
                .andExpect(content().string(containsString("inv2@alpha.test")))
                .andExpect(content().string(containsString("support@alpha.test")));

        // Filtered to INVESTIGATOR: the support user is excluded.
        mockMvc.perform(get(USERS).param("role", "INVESTIGATOR")
                        .header("Authorization", "Bearer " + tokenFor(1L, tenant, adminRole)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("inv1@alpha.test")))
                .andExpect(content().string(not(containsString("support@alpha.test"))));
    }

    @Test
    void tenantIsolationOnUserDirectory() throws Exception {
        long tenantA = insertCompany("Alpha", "ALPHA", "alpha");
        long tenantB = insertCompany("Beta", "BETA", "beta");
        long adminRole = roleIdByCode("TENANT_ADMIN");
        insertUser(tenantA, "onlyA@alpha.test", "h", roleIdByCode("INVESTIGATOR"));

        // Tenant B cannot see tenant A's users.
        mockMvc.perform(get(USERS).header("Authorization", "Bearer " + tokenFor(1L, tenantB, adminRole)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("onlyA@alpha.test"))));
    }

    @Test
    void withoutUserReadPermissionIsForbidden() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        long investigatorRole = roleIdByCode("INVESTIGATOR"); // no USER_READ

        mockMvc.perform(get(USERS).header("Authorization", "Bearer " + tokenFor(1L, tenant, investigatorRole)))
                .andExpect(status().isForbidden());
    }
}
