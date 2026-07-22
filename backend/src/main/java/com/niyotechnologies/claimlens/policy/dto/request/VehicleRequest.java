package com.niyotechnologies.claimlens.policy.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record VehicleRequest(
        @NotBlank(message = "Registration number is required")
        @Size(max = 32)
        String registrationNumber,

        @NotBlank(message = "Make is required")
        @Size(max = 100)
        String make,

        @NotBlank(message = "Model is required")
        @Size(max = 100)
        String model,

        @Size(max = 100) String variant,
        Integer manufactureYear,
        @Size(max = 64) String chassisNumber,
        @Size(max = 64) String engineNumber,
        @Size(max = 50) String colour,
        @Size(max = 30) String fuelType,
        Integer seatingCapacity,
        BigDecimal idv
) {
}
