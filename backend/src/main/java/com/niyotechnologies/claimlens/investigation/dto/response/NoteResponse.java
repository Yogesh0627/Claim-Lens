package com.niyotechnologies.claimlens.investigation.dto.response;

import com.niyotechnologies.claimlens.investigation.enums.NoteSeverity;
import com.niyotechnologies.claimlens.investigation.enums.NoteType;

import java.time.Instant;

public record NoteResponse(
        Long id,
        Long claimId,
        NoteType noteType,
        String note,
        NoteSeverity severity,
        Long documentId,
        Long createdBy,
        Instant createdAt
) {
}
