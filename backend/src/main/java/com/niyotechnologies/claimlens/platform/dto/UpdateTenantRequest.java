package com.niyotechnologies.claimlens.platform.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Edit an existing tenant. {@code code} and {@code tenantKey} are intentionally NOT editable — they
 * are the tenant's stable identity (referenced in URLs, keys, and existing data). Status and
 * soft-delete are changed through their own dedicated endpoints, not here.
 */
public record UpdateTenantRequest(
        @NotBlank String name,
        String subscriptionPlan,
        @NotBlank String currency,
        @NotBlank String timezone,
        String contactEmail
) {
}
