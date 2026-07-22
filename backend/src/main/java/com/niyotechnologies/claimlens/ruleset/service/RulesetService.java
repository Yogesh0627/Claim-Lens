package com.niyotechnologies.claimlens.ruleset.service;

import com.niyotechnologies.claimlens.ruleset.dto.request.CreateFraudRulesetRequest;
import com.niyotechnologies.claimlens.ruleset.dto.response.FraudRulesetResponse;

import java.util.List;

public interface RulesetService {

    FraudRulesetResponse createFraudRuleset(CreateFraudRulesetRequest request);

    FraudRulesetResponse activateFraudRuleset(Long rulesetId);

    FraudRulesetResponse getFraudRuleset(Long rulesetId);

    List<FraudRulesetResponse> getFraudRulesets();
}
