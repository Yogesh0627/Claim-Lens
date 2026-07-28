package com.niyotechnologies.claimlens.claim.dto.request;

/**
 * Move a claim already under investigation to a different investigator. A null investigatorUserId
 * means "auto-pick" (least-loaded eligible investigator), mirroring the auto-assign option.
 */
public record ReassignClaimRequest(
        Long investigatorUserId
) {
}
