package com.niyotechnologies.claimlens.document.dto.response;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        Long id,
        UUID publicId,
        Long claimId,
        String documentType,
        String fileName,
        String contentType,
        Long sizeBytes,
        String status,
        Instant createdAt
) {
}
