package com.niyotechnologies.claimlens.user.dto;

import jakarta.validation.constraints.NotBlank;

/** Self-service edit of one's own profile — name and phone only. Email and role are not self-editable. */
public record UpdateProfileRequest(
        @NotBlank String firstName,
        String lastName,
        String phone
) {
}
