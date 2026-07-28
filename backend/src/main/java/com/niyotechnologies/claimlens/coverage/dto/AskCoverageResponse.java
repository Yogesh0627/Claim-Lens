package com.niyotechnologies.claimlens.coverage.dto;

import java.util.List;

public record AskCoverageResponse(
        String answer,
        String model,
        Long insuranceProductVersionId,
        List<Citation> citations
) {

    public record Citation(
            Long policyChunkId,
            Integer chunkIndex,
            String snippet,
            Double score
    ) {
    }
}
