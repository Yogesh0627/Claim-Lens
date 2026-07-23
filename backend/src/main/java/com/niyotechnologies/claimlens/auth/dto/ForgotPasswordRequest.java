package com.niyotechnologies.claimlens.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Request a reset link. The response is always the same, whether or not the email exists. */
public record ForgotPasswordRequest(
        @NotBlank @Email String email
) {
}
