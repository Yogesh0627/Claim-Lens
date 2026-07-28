package com.niyotechnologies.claimlens.fraud.dto;

import java.util.List;

/**
 * One labelled data point for evaluating the fraud scorer on REAL outcomes: the score the engine gave,
 * which rules fired, and the ground-truth label the investigator recorded at decision time. The
 * evaluation harness consumes a list of these to compute precision/recall/AUC on live usage.
 */
public record FraudLabelResponse(
        Long claimId,
        String claimNumber,
        Integer score,
        String riskLevel,
        List<String> firedRules,
        boolean fraudConfirmed
) {
}
