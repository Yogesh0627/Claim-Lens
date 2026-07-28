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
 * Fires when a claim photo's EXIF metadata is INCONSISTENT with the reported incident (e.g. captured
 * before the incident date, or GPS far from the reported location). Deliberately only INCONSISTENT
 * raises the score — MISSING/UNKNOWN EXIF must NOT (social apps strip metadata, so absence is normal,
 * not suspicious). Consumes the analysis service's three-state exifState signal.
 */
@Component
@RequiredArgsConstructor
public class ExifInconsistentRule implements FraudRuleEvaluator {

    @Autowired
    private final AnalysisResultRepository analysisResultRepository;

    @Override
    public String code() {
        return FraudRuleCodes.EXIF_INCONSISTENT;
    }

    @Override
    public String description() {
        return "Photo metadata (EXIF) is inconsistent with the reported incident";
    }

    @Override
    public int defaultWeight() {
        return 20;
    }

    @Override
    public boolean triggers(Claim claim, InsurancePolicy policy) {
        return analysisResultRepository.findAllByClaimIdOrderByCreatedAtAsc(claim.getId())
                .stream().anyMatch(r -> "INCONSISTENT".equalsIgnoreCase(r.getExifState()));
    }
}
