package com.niyotechnologies.claimlens.coverage.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * A coverage question. Provide either a claimId (resolves to the claim's pinned product version) or an
 * insuranceProductVersionId directly.
 */
public record AskCoverageRequest(
        Long insuranceProductVersionId,
        Long claimId,
        @NotBlank String question
) {
}
