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
 * Fires when a claim photo appears AI-generated / synthetic. This is a SOFT signal by design —
 * synthetic-image detection is an arms race with real false-positive risk, so it flags for an
 * investigator, never auto-rejects. Consumes the analysis service's syntheticSignal.
 */
@Component
@RequiredArgsConstructor
public class SyntheticImageRule implements FraudRuleEvaluator {

    @Autowired
    private final AnalysisResultRepository analysisResultRepository;

    @Override
    public String code() {
        return FraudRuleCodes.SYNTHETIC_IMAGE;
    }

    @Override
    public String description() {
        return "A claim photo appears AI-generated or synthetic";
    }

    @Override
    public int defaultWeight() {
        return 25;
    }

    @Override
    public boolean triggers(Claim claim, InsurancePolicy policy) {
        return analysisResultRepository.findAllByClaimIdOrderByCreatedAtAsc(claim.getId())
                .stream().anyMatch(r -> Boolean.TRUE.equals(r.getSyntheticSignal()));
    }
}
