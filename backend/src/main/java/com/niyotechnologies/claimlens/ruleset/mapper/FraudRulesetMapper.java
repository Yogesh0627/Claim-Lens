package com.niyotechnologies.claimlens.ruleset.mapper;

import com.niyotechnologies.claimlens.ruleset.dto.request.CreateFraudRulesetRequest;
import com.niyotechnologies.claimlens.ruleset.dto.request.FraudRuleInput;
import com.niyotechnologies.claimlens.ruleset.dto.response.FraudRuleResponse;
import com.niyotechnologies.claimlens.ruleset.dto.response.FraudRulesetResponse;
import com.niyotechnologies.claimlens.ruleset.entity.FraudRule;
import com.niyotechnologies.claimlens.ruleset.entity.FraudRuleset;
import com.niyotechnologies.claimlens.ruleset.enums.RulesetStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FraudRulesetMapper {

    public FraudRuleset toEntity(CreateFraudRulesetRequest request, Long claimTypeId) {
        FraudRuleset ruleset = new FraudRuleset();
        ruleset.setClaimTypeId(claimTypeId);
        ruleset.setName(request.name().trim());
        ruleset.setMediumThreshold(request.mediumThreshold());
        ruleset.setHighThreshold(request.highThreshold());
        ruleset.setStatus(RulesetStatus.DRAFT);
        return ruleset;
    }

    public FraudRule toRule(FraudRuleInput input, Long rulesetId) {
        FraudRule rule = new FraudRule();
        rule.setFraudRulesetId(rulesetId);
        rule.setCode(input.code().trim());
        rule.setDescription(input.description());
        rule.setWeight(input.weight());
        rule.setEnabled(input.enabled() == null ? Boolean.TRUE : input.enabled());
        return rule;
    }

    public FraudRulesetResponse toResponse(FraudRuleset ruleset, List<FraudRule> rules) {
        return new FraudRulesetResponse(
                ruleset.getId(), ruleset.getClaimTypeId(), ruleset.getName(),
                ruleset.getMediumThreshold(), ruleset.getHighThreshold(), ruleset.getStatus(),
                rules.stream().map(this::ruleToResponse).toList());
    }

    public FraudRuleResponse ruleToResponse(FraudRule rule) {
        return new FraudRuleResponse(rule.getId(), rule.getCode(), rule.getDescription(),
                rule.getWeight(), rule.getEnabled());
    }
}
