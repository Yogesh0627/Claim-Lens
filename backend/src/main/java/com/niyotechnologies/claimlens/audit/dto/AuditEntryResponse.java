package com.niyotechnologies.claimlens.audit.dto;

import java.time.Instant;

public record AuditEntryResponse(
        Long id,
        String action,
        String entityType,
        Long entityId,
        Long userId,
        Instant createdAt
) {
}
