package com.niyotechnologies.claimlens.security;

import com.niyotechnologies.claimlens.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves @PreAuthorize enforcement is driven by the role's permissions (resolved server-side from
 * role_permission, not from the token). TENANT_ADMIN has ORG_REGION_WRITE; AUDITOR is read-only.
 */
class RbacIntegrationTest extends AbstractIntegrationTest {

    private static final String REGIONS = "/api/v1/organizations/regions";
    private static final String CREATE_BODY =
            "{\"code\":\"WEST\",\"name\":\"Western Region\",\"status\":\"ACTIVE\"}";

    @Test
    void adminCanCreateRegion() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        long adminRole = roleIdByCode("TENANT_ADMIN");

        mockMvc.perform(post(REGIONS)
                        .header("Authorization", "Bearer " + tokenFor(1L, tenant, adminRole))
                        .contentType("application/json")
                        .content(CREATE_BODY))
                .andExpect(status().isCreated());
    }

    @Test
    void auditorCannotCreateRegionButCanRead() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        insertRegion(tenant, "RA", "Region Alpha");
        long auditorRole = roleIdByCode("AUDITOR");

        // No ORG_REGION_WRITE → 403
        mockMvc.perform(post(REGIONS)
                        .header("Authorization", "Bearer " + tokenFor(1L, tenant, auditorRole))
                        .contentType("application/json")
                        .content(CREATE_BODY))
                .andExpect(status().isForbidden());

        // Has ORG_REGION_READ → 200
        mockMvc.perform(get(REGIONS)
                        .header("Authorization", "Bearer " + tokenFor(1L, tenant, auditorRole)))
                .andExpect(status().isOk());
    }

    @Test
    void roleWithNoOrgPermissionsIsForbidden() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        insertRegion(tenant, "RA", "Region Alpha");
        long investigatorRole = roleIdByCode("INVESTIGATOR"); // no org permissions granted

        mockMvc.perform(get(REGIONS)
                        .header("Authorization", "Bearer " + tokenFor(1L, tenant, investigatorRole)))
                .andExpect(status().isForbidden());
    }
}
