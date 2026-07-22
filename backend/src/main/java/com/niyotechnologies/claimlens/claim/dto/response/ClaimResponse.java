package com.niyotechnologies.claimlens.claim.dto.response;

import com.niyotechnologies.claimlens.claim.enums.ClaimStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ClaimResponse(
        Long id,
        UUID publicId,
        String claimNumber,
        Long customerId,
        Long insurancePolicyId,
        Long insuranceProductVersionId,
        String policyNumber,
        String vehicleRegistrationNumber,
        LocalDate incidentDate,
        BigDecimal claimAmount,
        ClaimStatus status,
        Instant submittedAt,
        /** Soft-validation signals raised at submit (empty otherwise) — never a rejection. */
        List<String> warnings
) {
}
