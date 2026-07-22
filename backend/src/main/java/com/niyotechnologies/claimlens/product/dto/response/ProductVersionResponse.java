package com.niyotechnologies.claimlens.product.dto.response;

import com.niyotechnologies.claimlens.product.enums.ProductVersionStatus;

import java.time.Instant;
import java.time.LocalDate;

public record ProductVersionResponse(
        Long id,
        Long insuranceProductId,
        Integer versionNumber,
        ProductVersionStatus status,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        String coverageSummary,
        Instant createdAt
) {
}
