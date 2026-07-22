package com.niyotechnologies.claimlens.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Create a back-office user. If a password is given the account is ACTIVE (can sign in immediately);
 * otherwise it is INVITED (e.g. to be used with Google sign-in on the same email).
 */
public record CreateUserRequest(
        @NotBlank @Email String email,
        @NotBlank String firstName,
        String lastName,
        @NotBlank String employeeCode,
        String phone,
        @NotBlank String roleCode,
        @Size(min = 6, max = 100) String password
) {
}
