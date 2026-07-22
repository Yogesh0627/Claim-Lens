package com.niyotechnologies.claimlens.document.dto.response;

import java.time.Instant;

public record DocumentVersionResponse(
        Long id,
        Long documentId,
        Integer versionNumber,
        String fileName,
        String contentType,
        Long sizeBytes,
        String status,
        boolean current,
        Instant createdAt
) {
}
