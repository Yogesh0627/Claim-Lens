package com.niyotechnologies.claimlens.portal.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * A policyholder's coverage question, scoped to one of their OWN policies. The server resolves the
 * product version from the (ownership-checked) policy — the customer never supplies a version id, so
 * they can only ever ask about coverage they actually hold.
 */
public record PortalAskCoverageRequest(
        @NotNull(message = "A policy is required") Long policyId,
        @NotBlank(message = "A question is required") String question
) {
}
