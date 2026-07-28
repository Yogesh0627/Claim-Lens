package com.niyotechnologies.claimlens.fraud.rule.impl;

import com.niyotechnologies.claimlens.claim.entity.Claim;
import com.niyotechnologies.claimlens.fraud.rule.FraudRuleCodes;
import com.niyotechnologies.claimlens.fraud.rule.FraudRuleEvaluator;
import com.niyotechnologies.claimlens.policy.entity.InsurancePolicy;
import com.niyotechnologies.claimlens.processing.repository.AnalysisResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Fires when a claim photo matches a photo already used on ANOTHER claim (perceptual-hash reuse) — the
 * top motor-fraud pattern. Consumes the analysis service's {@code duplicateOfClaimId} signal, which was
 * previously computed and stored but never influenced the fraud score.
 */
@Component
@RequiredArgsConstructor
public class DuplicateImageRule implements FraudRuleEvaluator {

    @Autowired
    private final AnalysisResultRepository analysisResultRepository;

    @Override
    public String code() {
        return FraudRuleCodes.DUPLICATE_IMAGE;
    }

    @Override
    public String description() {
        return "A claim photo matches a photo used on another claim (image reuse)";
    }

    @Override
    public int defaultWeight() {
        return 40;
    }

    @Override
    public boolean triggers(Claim claim, InsurancePolicy policy) {
        return analysisResultRepository.findAllByClaimIdOrderByCreatedAtAsc(claim.getId())
                .stream().anyMatch(r -> r.getDuplicateOfClaimId() != null);
    }
}
