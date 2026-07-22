package com.niyotechnologies.claimlens.claim.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * An investigator's request to the policyholder for more information/documents on a claim under
 * investigation. The message is what the customer sees (in-app + email) explaining what's needed.
 */
public record RequestInformationRequest(
        @NotBlank(message = "A message describing what's needed is required")
        String message
) {
}
