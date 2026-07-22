package com.niyotechnologies.claimlens.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.niyotechnologies.claimlens.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the real login/refresh flow end-to-end: seed a user with a BCrypt hash, log in over
 * HTTP, and use the returned access token against a protected, RBAC-guarded endpoint.
 */
class AuthIntegrationTest extends AbstractIntegrationTest {

    private static final String LOGIN = "/api/v1/auth/login";
    private static final String REFRESH = "/api/v1/auth/refresh-token";
    private static final String REGIONS = "/api/v1/organizations/regions";

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void loginSucceedsAndAccessTokenReachesProtectedEndpoint() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        insertUser(tenant, "admin@alpha.test", passwordEncoder.encode("password"),
                roleIdByCode("TENANT_ADMIN"));

        MvcResult result = mockMvc.perform(post(LOGIN)
                        .contentType("application/json")
                        .content("{\"email\":\"admin@alpha.test\",\"password\":\"password\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        String accessToken = data.get("accessToken").asText();

        // TENANT_ADMIN has ORG_REGION_READ → the token works through the real filter chain + RBAC.
        mockMvc.perform(get(REGIONS).header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    void loginWithWrongPasswordIsUnauthorized() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        insertUser(tenant, "admin@alpha.test", passwordEncoder.encode("password"),
                roleIdByCode("TENANT_ADMIN"));

        mockMvc.perform(post(LOGIN)
                        .contentType("application/json")
                        .content("{\"email\":\"admin@alpha.test\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithUnknownEmailIsUnauthorized() throws Exception {
        mockMvc.perform(post(LOGIN)
                        .contentType("application/json")
                        .content("{\"email\":\"nobody@alpha.test\",\"password\":\"password\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshRotatesAndOldTokenIsRejected() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        insertUser(tenant, "admin@alpha.test", passwordEncoder.encode("password"),
                roleIdByCode("TENANT_ADMIN"));

        MvcResult login = mockMvc.perform(post(LOGIN)
                        .contentType("application/json")
                        .content("{\"email\":\"admin@alpha.test\",\"password\":\"password\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String refreshToken = objectMapper.readTree(login.getResponse().getContentAsString())
                .get("data").get("refreshToken").asText();

        // Rotation issues new tokens.
        mockMvc.perform(post(REFRESH)
                        .contentType("application/json")
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk());

        // The old refresh token was revoked by rotation → rejected.
        mockMvc.perform(post(REFRESH)
                        .contentType("application/json")
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized());
    }
}
