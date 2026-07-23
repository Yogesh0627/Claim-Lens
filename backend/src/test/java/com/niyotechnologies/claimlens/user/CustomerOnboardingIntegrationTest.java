package com.niyotechnologies.claimlens.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.niyotechnologies.claimlens.auth.enums.InvitationPurpose;
import com.niyotechnologies.claimlens.auth.service.InvitationService;
import com.niyotechnologies.claimlens.support.AbstractIntegrationTest;
import com.niyotechnologies.claimlens.user.entity.AppUser;
import com.niyotechnologies.claimlens.user.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Onboarding a policyholder end-to-end through the API alone — the flow that previously required
 * hand-written SQL.
 *
 * <p>Three defects are pinned here:
 * <ol>
 *   <li>a CUSTOMER login was created with a null customer_id, so the portal was permanently empty;</li>
 *   <li>an account created without a password became INVITED and could never sign in by any route;</li>
 *   <li>there was no way for a person to set their own password — an admin had to pick one for them.</li>
 * </ol>
 */
class CustomerOnboardingIntegrationTest extends AbstractIntegrationTest {

    private final ObjectMapper om = new ObjectMapper();

    @Autowired
    private InvitationService invitationService;
    @Autowired
    private AppUserRepository appUserRepository;

    private String createUserBody(String email, String role, Long customerId, String password) {
        return "{\"email\":\"" + email + "\",\"firstName\":\"Riya\",\"lastName\":\"Verma\","
                + "\"employeeCode\":\"EMP-" + email.hashCode() + "\",\"roleCode\":\"" + role + "\""
                + (customerId == null ? "" : ",\"customerId\":" + customerId)
                + (password == null ? "" : ",\"password\":\"" + password + "\"")
                + "}";
    }

    @Test
    void customerLoginIsLinkedToTheCustomerAndCanSetItsOwnPassword() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        String auth = "Bearer " + tokenFor(1L, tenant, roleIdByCode("TENANT_ADMIN"));
        long customerId = insertCustomer(tenant, "CUST-A");

        // No password -> INVITED, and the login is linked to the policyholder.
        MvcResult created = mockMvc.perform(post("/api/v1/users").header("Authorization", auth)
                        .contentType("application/json")
                        .content(createUserBody("riya@alpha.test", "CUSTOMER", customerId, null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("INVITED"))
                .andReturn();
        long userId = om.readTree(created.getResponse().getContentAsString()).get("data").get("id").asLong();

        AppUser user = appUserRepository.findByIdForAuthentication(userId).orElseThrow();
        assertEquals(customerId, user.getCustomerId(), "portal login must point at the customer");

        // INVITED cannot sign in yet — there is no password.
        mockMvc.perform(post("/api/v1/auth/login").contentType("application/json")
                        .content("{\"email\":\"riya@alpha.test\",\"password\":\"Password1\"}"))
                .andExpect(status().isUnauthorized());

        // Redeem the invitation to choose a password -> account becomes usable.
        String token = invitationService.issueToken(user, InvitationPurpose.INVITE);
        mockMvc.perform(post("/api/v1/auth/set-password").contentType("application/json")
                        .content("{\"token\":\"" + token + "\",\"password\":\"Password1\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/login").contentType("application/json")
                        .content("{\"email\":\"riya@alpha.test\",\"password\":\"Password1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists());
    }

    @Test
    void aCustomerLoginWithoutACustomerIsRejected() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        String auth = "Bearer " + tokenFor(1L, tenant, roleIdByCode("TENANT_ADMIN"));

        // The exact bug: a CUSTOMER account with no customer would sign in to an empty portal.
        mockMvc.perform(post("/api/v1/users").header("Authorization", auth)
                        .contentType("application/json")
                        .content(createUserBody("orphan@alpha.test", "CUSTOMER", null, "Password1")))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void staffCannotBeLinkedToACustomer() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        String auth = "Bearer " + tokenFor(1L, tenant, roleIdByCode("TENANT_ADMIN"));
        long customerId = insertCustomer(tenant, "CUST-A");

        mockMvc.perform(post("/api/v1/users").header("Authorization", auth)
                        .contentType("application/json")
                        .content(createUserBody("inv@alpha.test", "INVESTIGATOR", customerId, "Password1")))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void anInvitationTokenIsSingleUse() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        long userId = insertUser(tenant, "once@alpha.test", "hash", roleIdByCode("INVESTIGATOR"));
        AppUser user = appUserRepository.findByIdForAuthentication(userId).orElseThrow();
        String token = invitationService.issueToken(user, InvitationPurpose.RESET);

        mockMvc.perform(post("/api/v1/auth/set-password").contentType("application/json")
                        .content("{\"token\":\"" + token + "\",\"password\":\"Password1\"}"))
                .andExpect(status().isNoContent());

        // Replaying the same link must fail.
        mockMvc.perform(post("/api/v1/auth/set-password").contentType("application/json")
                        .content("{\"token\":\"" + token + "\",\"password\":\"Password2\"}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void forgotPasswordNeverRevealsWhetherAnAccountExists() throws Exception {
        long tenant = insertCompany("Alpha", "ALPHA", "alpha");
        insertUser(tenant, "known@alpha.test", "hash", roleIdByCode("INVESTIGATOR"));

        // Identical response for both, or this becomes an account-enumeration oracle.
        mockMvc.perform(post("/api/v1/auth/forgot-password").contentType("application/json")
                        .content("{\"email\":\"known@alpha.test\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/auth/forgot-password").contentType("application/json")
                        .content("{\"email\":\"nobody@nowhere.test\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void anInvalidTokenIsRejected() throws Exception {
        assertNotNull(invitationService);
        mockMvc.perform(post("/api/v1/auth/set-password").contentType("application/json")
                        .content("{\"token\":\"not-a-real-token\",\"password\":\"Password1\"}"))
                .andExpect(status().is4xxClientError());
    }
}
