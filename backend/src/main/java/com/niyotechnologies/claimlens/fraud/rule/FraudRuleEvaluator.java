package com.niyotechnologies.claimlens.fraud.rule;

import com.niyotechnologies.claimlens.claim.entity.Claim;
import com.niyotechnologies.claimlens.policy.entity.InsurancePolicy;

/**
 * A single named fraud check. Config (fraud_rule) decides whether it is enabled and what weight it
 * contributes; this class only decides whether it fires for a given claim. Add a rule = add an
 * implementation + a fraud_rule row; no engine change.
 */
public interface FraudRuleEvaluator {

    /** Stable code that a fraud_rule row references (e.g. AMOUNT_OVER_SUM_INSURED). */
    String code();

    boolean triggers(Claim claim, InsurancePolicy policy);
}
