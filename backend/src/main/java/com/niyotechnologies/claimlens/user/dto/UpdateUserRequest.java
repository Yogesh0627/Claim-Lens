package com.niyotechnologies.claimlens.user.dto;

import jakarta.validation.constraints.NotBlank;

/** Update a user's profile, role or status (ACTIVE | SUSPENDED | TERMINATED | INVITED). */
public record UpdateUserRequest(
        @NotBlank String firstName,
        String lastName,
        String phone,
        @NotBlank String roleCode,
        @NotBlank String status
) {
}
