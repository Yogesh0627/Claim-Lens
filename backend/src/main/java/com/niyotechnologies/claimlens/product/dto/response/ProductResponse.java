package com.niyotechnologies.claimlens.product.dto.response;

import com.niyotechnologies.claimlens.product.enums.ProductStatus;

import java.time.Instant;

public record ProductResponse(
        Long id,
        String code,
        String name,
        String description,
        Long claimTypeId,
        ProductStatus status,
        Instant createdAt
) {
}
