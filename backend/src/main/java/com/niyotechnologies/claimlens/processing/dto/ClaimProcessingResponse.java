package com.niyotechnologies.claimlens.processing.dto;

import java.util.List;

/** Everything the "Processing" view needs for a claim: pipeline state, fraud score, and per-document
 * OCR text and image analysis signals. */
public record ClaimProcessingResponse(
        ProcessingState state,
        FraudSummary fraud,
        List<OcrResultResponse> ocrResults,
        List<AnalysisResultResponse> analysisResults
) {

    public record ProcessingState(String ocrStatus, String analysisStatus, String fraudStatus) {
    }

    public record FraudSummary(Integer score, String riskLevel, String explanation) {
    }
}
