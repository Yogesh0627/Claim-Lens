package com.niyotechnologies.claimlens.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** The Google ID token ("credential") returned to the frontend by Google Sign-In. */
public record GoogleLoginRequest(
        @NotBlank String credential
) {
}
