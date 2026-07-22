package com.niyotechnologies.claimlens.platform.dto;

import jakarta.validation.constraints.NotBlank;

/** Change a tenant's lifecycle status: ACTIVE | SUSPENDED | ONBOARDING. */
public record TenantStatusRequest(
        @NotBlank String status
) {
}
