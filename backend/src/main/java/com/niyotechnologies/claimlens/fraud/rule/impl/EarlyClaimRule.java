package com.niyotechnologies.claimlens.fraud.rule.impl;

import com.niyotechnologies.claimlens.claim.entity.Claim;
import com.niyotechnologies.claimlens.fraud.rule.FraudRuleCodes;
import com.niyotechnologies.claimlens.fraud.rule.FraudRuleEvaluator;
import com.niyotechnologies.claimlens.policy.entity.InsurancePolicy;
import org.springframework.stereotype.Component;

/** Fires when the incident happens within 30 days of the policy starting. */
@Component
public class EarlyClaimRule implements FraudRuleEvaluator {

    @Override
    public String code() {
        return FraudRuleCodes.EARLY_CLAIM;
    }

    @Override
    public String description() {
        return "Claim filed within 30 days of the policy starting";
    }

    @Override
    public int defaultWeight() {
        return 20;
    }

    @Override
    public boolean triggers(Claim claim, InsurancePolicy policy) {
        return policy != null
                && claim.getIncidentDate() != null
                && policy.getEffectiveFrom() != null
                && claim.getIncidentDate().isBefore(policy.getEffectiveFrom().plusDays(30));
    }
}
