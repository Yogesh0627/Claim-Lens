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
        /** The policyholder the claim is for — name + customer number, resolved from customerId. */
        String customerName,
        String customerNumber,
        /**
         * Who actually filed the claim. For self-service it's the customer; when an employee or
         * customer-support agent files on a customer's behalf, it's that staff member (with their
         * employee code and raisedByStaff=true).
         */
        String raisedByName,
        String raisedByCode,
        boolean raisedByStaff,
        /** The investigator currently assigned to the claim (null until assigned) — name + employee code. */
        String investigatingOfficerName,
        String investigatingOfficerCode,
        Long insurancePolicyId,
        Long insuranceProductVersionId,
        /** Resolved from the pinned version so the UI shows "Demo Motor Comprehensive · v1", not raw ids. */
        String productName,
        Integer productVersionNumber,
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
