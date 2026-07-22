package com.niyotechnologies.claimlens.platform.dto;

import jakarta.validation.constraints.NotBlank;

/** Onboard a new tenant (insurance company). Created in ONBOARDING status. */
public record OnboardTenantRequest(
        @NotBlank String name,
        @NotBlank String code,
        @NotBlank String tenantKey,
        String subscriptionPlan,
        @NotBlank String currency,
        @NotBlank String timezone,
        String contactEmail
) {
}
