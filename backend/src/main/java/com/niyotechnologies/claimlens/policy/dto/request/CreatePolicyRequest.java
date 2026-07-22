package com.niyotechnologies.claimlens.policy.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Note: the client does NOT choose the product version — the policy is pinned to the product's
 * currently ACTIVE version at creation time (see PolicyServiceImpl).
 */
public record CreatePolicyRequest(
        @NotBlank(message = "Policy number is required")
        @Size(max = 100)
        String policyNumber,

        @NotNull(message = "Customer is required")
        Long customerId,

        @NotNull(message = "Product is required")
        Long insuranceProductId,

        @NotNull(message = "Effective-from date is required")
        LocalDate effectiveFrom,

        @NotNull(message = "Effective-to date is required")
        LocalDate effectiveTo,

        @NotNull(message = "Sum insured is required")
        @Positive(message = "Sum insured must be positive")
        BigDecimal sumInsured,

        BigDecimal deductible,
        BigDecimal premiumAmount,
        @Size(max = 3) String currency,

        @NotNull(message = "Vehicle is required")
        @Valid
        VehicleRequest vehicle
) {
}
