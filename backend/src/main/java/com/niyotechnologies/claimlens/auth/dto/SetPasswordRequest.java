package com.niyotechnologies.claimlens.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Redeem an invitation or reset link: the token is the credential, so no email is needed. */
public record SetPasswordRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 6, max = 100) String password
) {
}
