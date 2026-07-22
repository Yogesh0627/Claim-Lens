package com.niyotechnologies.claimlens.claim.dto.request;

import jakarta.validation.constraints.NotNull;

public record AssignClaimRequest(
        @NotNull(message = "Investigator is required")
        Long investigatorUserId
) {
}
