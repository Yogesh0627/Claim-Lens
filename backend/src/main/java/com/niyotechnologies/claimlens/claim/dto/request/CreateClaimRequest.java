package com.niyotechnologies.claimlens.claim.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateClaimRequest(
        @NotNull(message = "Customer is required")
        Long customerId,

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
