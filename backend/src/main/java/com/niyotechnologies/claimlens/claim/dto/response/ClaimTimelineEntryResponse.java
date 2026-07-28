package com.niyotechnologies.claimlens.claim.dto.response;

import java.time.Instant;

/**
 * One step in a claim's journey — a status transition with its note and timestamp. Surfaced to the
 * policyholder as a progress tracker, so "Information requested: please send the RC book" and the
 * approval reason reach the customer without exposing the staff audit log.
 */
public record ClaimTimelineEntryResponse(
        String fromStatus,
        String toStatus,
        String note,
        Instant at
) {
}
