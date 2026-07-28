package com.niyotechnologies.claimlens.coverage.dto;

/**
 * A product version the AI assistant can actually answer about — i.e. one that has policy wording
 * ingested. Used to populate the staff assistant's "which product?" picker.
 */
public record AskableProductResponse(
        Long productId,
        String productName,
        String productCode,
        Long versionId,
        Integer versionNumber
) {
}
