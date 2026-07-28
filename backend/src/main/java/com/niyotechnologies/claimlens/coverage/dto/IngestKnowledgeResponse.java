package com.niyotechnologies.claimlens.coverage.dto;

public record IngestKnowledgeResponse(
        Long insuranceProductVersionId,
        long chunkCount,
        String embeddingModel
) {
}
