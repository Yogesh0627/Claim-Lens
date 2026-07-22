package com.niyotechnologies.claimlens.product.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateProductVersionRequest(
        @NotNull(message = "Effective from date is required")
        LocalDate effectiveFrom,

        LocalDate effectiveTo,

        String coverageSummary
) {
}
