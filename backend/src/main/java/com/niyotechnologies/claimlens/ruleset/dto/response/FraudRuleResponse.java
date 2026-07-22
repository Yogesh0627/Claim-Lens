package com.niyotechnologies.claimlens.ruleset.dto.response;

public record FraudRuleResponse(
        Long id,
        String code,
        String description,
        Integer weight,
        Boolean enabled
) {
}
