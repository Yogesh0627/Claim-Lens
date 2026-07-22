package com.niyotechnologies.claimlens.fraud.rule.impl;

import com.niyotechnologies.claimlens.claim.entity.Claim;
import com.niyotechnologies.claimlens.fraud.rule.FraudRuleCodes;
import com.niyotechnologies.claimlens.fraud.rule.FraudRuleEvaluator;
import com.niyotechnologies.claimlens.policy.entity.InsurancePolicy;
import org.springframework.stereotype.Component;

/** Fires when the claim amount exceeds the policy's sum insured. */
@Component
public class AmountOverSumInsuredRule implements FraudRuleEvaluator {

    @Override
    public String code() {
        return FraudRuleCodes.AMOUNT_OVER_SUM_INSURED;
    }

    @Override
    public boolean triggers(Claim claim, InsurancePolicy policy) {
        return policy != null
                && claim.getClaimAmount() != null
                && policy.getSumInsured() != null
                && claim.getClaimAmount().compareTo(policy.getSumInsured()) > 0;
    }
}
