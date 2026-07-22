package com.niyotechnologies.claimlens.policy.dto.response;

import com.niyotechnologies.claimlens.policy.enums.PolicyStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PolicyResponse(
        Long id,
        UUID publicId,
        String policyNumber,
        Long customerId,
        Long insuranceProductId,
        Long insuranceProductVersionId,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        BigDecimal sumInsured,
        BigDecimal deductible,
        BigDecimal premiumAmount,
        String currency,
        PolicyStatus status,
        Instant issuedAt,
        VehicleResponse vehicle
) {
}
