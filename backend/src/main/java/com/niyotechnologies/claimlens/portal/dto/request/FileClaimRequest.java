package com.niyotechnologies.claimlens.portal.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A policyholder filing their own claim. Deliberately has NO customerId — the customer is derived
 * from the authenticated principal, never trusted from the request body (that would let a customer
 * file against another customer).
 */
public record FileClaimRequest(
        @NotNull(message = "Policy is required")
        Long insurancePolicyId,

        @NotNull(message = "Incident date is required")
        LocalDate incidentDate,

        @Positive(message = "Claim amount must be positive")
        BigDecimal claimAmount,

        @Size(max = 32)
        String vehicleRegistrationNumber,

        String description
) {
}
