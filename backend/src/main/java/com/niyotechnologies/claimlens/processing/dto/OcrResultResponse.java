package com.niyotechnologies.claimlens.processing.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record OcrResultResponse(
        Long id,
        Long documentId,
        String engine,
        String extractedText,
        BigDecimal confidence,
        String registrationNumbers,
        String policyNumbers,
        Instant createdAt
) {
}
