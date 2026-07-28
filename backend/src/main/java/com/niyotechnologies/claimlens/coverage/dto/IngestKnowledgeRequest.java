package com.niyotechnologies.claimlens.coverage.dto;

import jakarta.validation.constraints.NotBlank;

/** Raw policy-wording text to ingest for a product version. */
public record IngestKnowledgeRequest(
        @NotBlank String text
) {
}
