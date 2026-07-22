package com.niyotechnologies.claimlens.auth;

import com.niyotechnologies.claimlens.auth.service.GoogleTokenVerifier;
import com.niyotechnologies.claimlens.auth.service.GoogleTokenVerifier.GoogleIdentity;
import com.niyotechnologies.claimlens.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Google sign-in: a verified Google identity logs into an EXISTING account (matched by the globally
 * unique email) and gets our normal JWT; an unknown email is rejected (we never auto-create tenant-
 * less accounts). Only the external Google verification is mocked.
 */
class GoogleLoginIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private GoogleTokenVerifier googleTokenVerifier;

    @Test
    void googleLoginIssuesJwtForExistingUser() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        insertUser(tenant, "guser@alpha.test", "hash", roleIdByCode("INVESTIGATOR"));
        when(googleTokenVerifier.verify(anyString()))
                .thenReturn(new GoogleIdentity("guser@alpha.test", "sub-123", "G User"));

        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType("application/json")
                        .content("{\"credential\":\"google-id-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists());
    }

    @Test
    void googleLoginRejectsEmailWithNoAccount() throws Exception {
        when(googleTokenVerifier.verify(anyString()))
                .thenReturn(new GoogleIdentity("stranger@nowhere.test", "sub-999", "Stranger"));

        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType("application/json")
                        .content("{\"credential\":\"google-id-token\"}"))
                .andExpect(status().isUnauthorized());
    }
}
