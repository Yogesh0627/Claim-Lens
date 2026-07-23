package com.niyotechnologies.claimlens.security;

import com.niyotechnologies.claimlens.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Brute-force protection on the one unauthenticated surface. Rate limiting is off for the rest of the
 * suite (so tests that log in repeatedly aren't throttled) and re-enabled here with a tiny window.
 */
@TestPropertySource(properties = {
        "claimlens.security.rate-limit.enabled=true",
        "claimlens.security.rate-limit.auth.limit=3",
        "claimlens.security.rate-limit.auth.window=PT1M"
})
class RateLimitIntegrationTest extends AbstractIntegrationTest {

    private static final String LOGIN = "/api/v1/auth/login";
    private static final String BAD_CREDENTIALS =
            "{\"email\":\"attacker@nowhere.test\",\"password\":\"guess\"}";

    @Test
    void repeatedFailedLoginsAreThrottledWith429() throws Exception {
        // Within the allowance: rejected on credentials, not on rate.
        for (int attempt = 1; attempt <= 3; attempt++) {
            mockMvc.perform(post(LOGIN).contentType("application/json").content(BAD_CREDENTIALS))
                    .andExpect(status().isUnauthorized());
        }

        // Over the allowance: refused before authentication even runs.
        mockMvc.perform(post(LOGIN).contentType("application/json").content(BAD_CREDENTIALS))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void throttlingIsScopedPerEndpointNotGlobally() throws Exception {
        // Exhaust the login bucket.
        for (int attempt = 1; attempt <= 4; attempt++) {
            mockMvc.perform(post(LOGIN).contentType("application/json").content(BAD_CREDENTIALS));
        }
        mockMvc.perform(post(LOGIN).contentType("application/json").content(BAD_CREDENTIALS))
                .andExpect(status().isTooManyRequests());

        // A different auth endpoint keeps its own bucket, so refresh still works for honest users.
        mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .contentType("application/json").content("{\"refreshToken\":\"nope\"}"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Only abuse-prone and cost-bearing endpoints are throttled. Ordinary reads must never be, or a
     * busy investigator working through a queue would start getting 429s.
     */
    @Test
    void ordinaryEndpointsAreNotThrottled() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        String auth = "Bearer " + tokenFor(1L, tenant, roleIdByCode("TENANT_ADMIN"));

        for (int i = 0; i < 25; i++) {
            mockMvc.perform(get("/api/v1/claims").header("Authorization", auth))
                    .andExpect(status().isOk());
        }
    }
}
