package com.niyotechnologies.claimlens.claim.dto.request;

import com.niyotechnologies.claimlens.claim.enums.ClaimDecision;
import jakarta.validation.constraints.NotNull;

public record ClaimDecisionRequest(
        @NotNull(message = "Decision is required")
        ClaimDecision decision,

        String reason
) {
}
