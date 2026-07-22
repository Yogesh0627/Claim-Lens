package com.niyotechnologies.claimlens.ruleset.dto.response;

import com.niyotechnologies.claimlens.ruleset.enums.RulesetStatus;

import java.util.List;

public record FraudRulesetResponse(
        Long id,
        Long claimTypeId,
        String name,
        Integer mediumThreshold,
        Integer highThreshold,
        RulesetStatus status,
        List<FraudRuleResponse> rules
) {
}
