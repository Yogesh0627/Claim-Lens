package com.niyotechnologies.claimlens.processing.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AnalysisResultResponse(
        Long id,
        Long documentId,
        String phash,
        String exifState,
        boolean syntheticSignal,
        BigDecimal syntheticScore,
        Long duplicateOfClaimId,
        Instant createdAt
) {
}
