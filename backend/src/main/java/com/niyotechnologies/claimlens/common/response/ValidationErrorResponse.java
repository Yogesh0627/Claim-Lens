package com.niyotechnologies.claimlens.common.response;

import java.time.Instant;
import java.util.Map;

public record ValidationErrorResponse(
        boolean success,
        String code,
        String message,
        Map<String, String> errors,
        Instant timestamp
) {

    public static ValidationErrorResponse of(
            Map<String, String> errors
    ) {
        return new ValidationErrorResponse(
                false,
                "VALIDATION_ERROR",
                "Validation failed",
                errors,
                Instant.now()
        );
    }
}