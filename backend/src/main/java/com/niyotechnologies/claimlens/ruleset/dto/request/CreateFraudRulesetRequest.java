package com.niyotechnologies.claimlens.ruleset.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

public record CreateFraudRulesetRequest(
        @NotBlank(message = "Claim type code is required")
        String claimTypeCode,

        @NotBlank(message = "Name is required")
        String name,

        @NotNull @PositiveOrZero
        Integer mediumThreshold,

        @NotNull @PositiveOrZero
        Integer highThreshold,

        @Valid
        List<FraudRuleInput> rules
) {
}
