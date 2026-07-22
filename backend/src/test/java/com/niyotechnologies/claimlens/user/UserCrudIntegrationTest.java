package com.niyotechnologies.claimlens.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.niyotechnologies.claimlens.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * User administration (create / update). Gated by USER_WRITE (tenant admin); tenant-scoped by @TenantId;
 * email is globally unique.
 */
class UserCrudIntegrationTest extends AbstractIntegrationTest {

    private final ObjectMapper om = new ObjectMapper();

    @Test
    void adminCreatesAndUpdatesUser() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        String auth = "Bearer " + tokenFor(1L, tenant, roleIdByCode("TENANT_ADMIN"));

        MvcResult created = mockMvc.perform(post("/api/v1/users").header("Authorization", auth)
                        .contentType("application/json")
                        .content("{\"email\":\"new.inv@alpha.test\",\"firstName\":\"New\",\"lastName\":\"Investigator\","
                                + "\"employeeCode\":\"EMP-NEW\",\"roleCode\":\"INVESTIGATOR\",\"password\":\"Password1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.roleCode").value("INVESTIGATOR"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andReturn();
        long userId = om.readTree(created.getResponse().getContentAsString()).get("data").get("id").asLong();

        // Promote to CLAIMS_MANAGER and suspend.
        mockMvc.perform(put("/api/v1/users/{id}", userId).header("Authorization", auth)
                        .contentType("application/json")
                        .content("{\"firstName\":\"New\",\"lastName\":\"Manager\",\"roleCode\":\"CLAIMS_MANAGER\",\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roleCode").value("CLAIMS_MANAGER"))
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"));
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        insertUser(tenant, "taken@alpha.test", "h", roleIdByCode("INVESTIGATOR"));
        String auth = "Bearer " + tokenFor(1L, tenant, roleIdByCode("TENANT_ADMIN"));

        mockMvc.perform(post("/api/v1/users").header("Authorization", auth)
                        .contentType("application/json")
                        .content("{\"email\":\"taken@alpha.test\",\"firstName\":\"Dup\",\"employeeCode\":\"EMP-D\","
                                + "\"roleCode\":\"INVESTIGATOR\",\"password\":\"Password1\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRequiresUserWrite() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        // INVESTIGATOR has neither USER_READ nor USER_WRITE.
        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + tokenFor(1L, tenant, roleIdByCode("INVESTIGATOR")))
                        .contentType("application/json")
                        .content("{\"email\":\"x@alpha.test\",\"firstName\":\"X\",\"employeeCode\":\"EMP-X\","
                                + "\"roleCode\":\"INVESTIGATOR\",\"password\":\"Password1\"}"))
                .andExpect(status().isForbidden());
    }
}
