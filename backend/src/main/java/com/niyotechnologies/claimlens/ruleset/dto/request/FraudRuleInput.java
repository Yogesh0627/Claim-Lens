package com.niyotechnologies.claimlens.ruleset.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record FraudRuleInput(
        @NotBlank(message = "Rule code is required")
        String code,

        String description,

        @NotNull(message = "Weight is required")
        @PositiveOrZero(message = "Weight must be zero or positive")
        Integer weight,

        Boolean enabled
) {
}
