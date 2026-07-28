package com.niyotechnologies.claimlens.fraud.engine;

import com.niyotechnologies.claimlens.claim.entity.Claim;
import com.niyotechnologies.claimlens.claim.enums.ClaimStatus;
import com.niyotechnologies.claimlens.claim.repository.ClaimRepository;
import com.niyotechnologies.claimlens.common.exception.NotFoundException;
import com.niyotechnologies.claimlens.fraud.entity.FraudScore;
import com.niyotechnologies.claimlens.fraud.repository.FraudScoreRepository;
import com.niyotechnologies.claimlens.fraud.rule.FraudRuleCodes;
import com.niyotechnologies.claimlens.fraud.rule.FraudRuleEvaluator;
import com.niyotechnologies.claimlens.fraud.rule.FraudRuleRegistry;
import com.niyotechnologies.claimlens.policy.entity.InsurancePolicy;
import com.niyotechnologies.claimlens.policy.repository.InsurancePolicyRepository;
import com.niyotechnologies.claimlens.ruleset.entity.FraudRule;
import com.niyotechnologies.claimlens.ruleset.entity.FraudRuleset;
import com.niyotechnologies.claimlens.ruleset.enums.RulesetStatus;
import com.niyotechnologies.claimlens.ruleset.repository.FraudRuleRepository;
import com.niyotechnologies.claimlens.ruleset.repository.FraudRulesetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rule-based, explainable fraud scoring (V1 — no ML). Loads the tenant's ACTIVE fraud ruleset for the
 * claim type and applies its enabled rules (each a named FraudRuleEvaluator with a configured weight),
 * comparing the total against the ruleset's thresholds. When no ruleset is configured it falls back to
 * sensible built-in defaults, so scoring works out of the box and can be customised without code.
 */
@Service
@RequiredArgsConstructor
public class FraudEngine {

    // Built-in defaults used when a tenant has not configured a fraud ruleset.
    private static final int DEFAULT_AMOUNT_WEIGHT = 40;
    private static final int DEFAULT_EARLY_WEIGHT = 20;
    private static final int DEFAULT_DUPLICATE_IMAGE_WEIGHT = 40;
    private static final int DEFAULT_SYNTHETIC_IMAGE_WEIGHT = 25;
    private static final int DEFAULT_EXIF_INCONSISTENT_WEIGHT = 20;
    private static final int DEFAULT_MEDIUM_THRESHOLD = 25;
    private static final int DEFAULT_HIGH_THRESHOLD = 50;

    @Autowired
    private final ClaimRepository claimRepository;
    @Autowired
    private final InsurancePolicyRepository policyRepository;
    @Autowired
    private final FraudScoreRepository fraudScoreRepository;
    @Autowired
    private final FraudRulesetRepository fraudRulesetRepository;
    @Autowired
    private final FraudRuleRepository fraudRuleRepository;
    @Autowired
    private final FraudRuleRegistry ruleRegistry;

    @Transactional
    public void evaluate(Long claimId) {
        Claim claim = claimRepository.findByIdAndIsDeletedFalse(claimId)
                .orElseThrow(() -> new NotFoundException("CLAIM_NOT_FOUND", "Claim not found"));
        InsurancePolicy policy = policyRepository.findByIdAndIsDeletedFalse(claim.getInsurancePolicyId())
                .orElse(null);

        FraudRuleset ruleset = fraudRulesetRepository
                .findByClaimTypeIdAndStatusAndIsDeletedFalse(claim.getClaimTypeId(), RulesetStatus.ACTIVE)
                .orElse(null);

        int score = 0;
        StringBuilder explanation = new StringBuilder();
        int mediumThreshold;
        int highThreshold;

        if (ruleset != null) {
            mediumThreshold = ruleset.getMediumThreshold();
            highThreshold = ruleset.getHighThreshold();
            for (FraudRule rule : fraudRuleRepository.findAllByFraudRulesetIdAndIsDeletedFalse(ruleset.getId())) {
                if (!Boolean.TRUE.equals(rule.getEnabled())) {
                    continue;
                }
                FraudRuleEvaluator evaluator = ruleRegistry.get(rule.getCode());
                if (evaluator != null && evaluator.triggers(claim, policy)) {
                    score += rule.getWeight();
                    explanation.append(rule.getCode()).append(" (+").append(rule.getWeight()).append("); ");
                }
            }
        } else {
            // No configured ruleset -> built-in defaults.
            mediumThreshold = DEFAULT_MEDIUM_THRESHOLD;
            highThreshold = DEFAULT_HIGH_THRESHOLD;
            score += applyDefault(FraudRuleCodes.AMOUNT_OVER_SUM_INSURED, DEFAULT_AMOUNT_WEIGHT, claim, policy, explanation);
            score += applyDefault(FraudRuleCodes.EARLY_CLAIM, DEFAULT_EARLY_WEIGHT, claim, policy, explanation);
            score += applyDefault(FraudRuleCodes.DUPLICATE_IMAGE, DEFAULT_DUPLICATE_IMAGE_WEIGHT, claim, policy, explanation);
            score += applyDefault(FraudRuleCodes.SYNTHETIC_IMAGE, DEFAULT_SYNTHETIC_IMAGE_WEIGHT, claim, policy, explanation);
            score += applyDefault(FraudRuleCodes.EXIF_INCONSISTENT, DEFAULT_EXIF_INCONSISTENT_WEIGHT, claim, policy, explanation);
        }

        String risk = score >= highThreshold ? "HIGH" : score >= mediumThreshold ? "MEDIUM" : "LOW";

        FraudScore fraudScore = new FraudScore();
        fraudScore.setTenantId(claim.getTenantId());
        fraudScore.setClaimId(claimId);
        fraudScore.setScore(score);
        fraudScore.setRiskLevel(risk);
        fraudScore.setExplanation(explanation.length() == 0 ? "No fraud signals" : explanation.toString());
        fraudScoreRepository.save(fraudScore);

        claim.setStatus(ClaimStatus.AWAITING_ASSIGNMENT);
        claimRepository.save(claim);
    }

    private int applyDefault(String code, int weight, Claim claim, InsurancePolicy policy,
                             StringBuilder explanation) {
        FraudRuleEvaluator evaluator = ruleRegistry.get(code);
        if (evaluator != null && evaluator.triggers(claim, policy)) {
            explanation.append(code).append(" (+").append(weight).append("); ");
            return weight;
        }
        return 0;
    }
}
