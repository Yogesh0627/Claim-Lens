package com.niyotechnologies.claimlens.claim.dto.request;

import com.niyotechnologies.claimlens.claim.enums.ClaimDecision;
import jakarta.validation.constraints.NotNull;

public record ClaimDecisionRequest(
        @NotNull(message = "Decision is required")
        ClaimDecision decision,

        String reason,

        /**
         * Ground-truth fraud label for the evaluation feedback loop. On REJECT, whether the claim was
         * actually fraudulent (vs denied for a coverage reason). Ignored on APPROVE (a paid claim is
         * recorded as not-fraud). Optional — defaults to false.
         */
        Boolean fraudConfirmed
) {
}
