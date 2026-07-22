package com.niyotechnologies.claimlens.policy.dto.response;

import com.niyotechnologies.claimlens.policy.enums.VehicleStatus;

import java.math.BigDecimal;

public record VehicleResponse(
        Long id,
        String registrationNumber,
        String make,
        String model,
        String variant,
        Integer manufactureYear,
        String chassisNumber,
        String engineNumber,
        String colour,
        String fuelType,
        Integer seatingCapacity,
        BigDecimal idv,
        VehicleStatus status
) {
}
