package com.niyotechnologies.claimlens.customer.dto.response;

import com.niyotechnologies.claimlens.customer.enums.CustomerStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CustomerResponse(
        Long id,
        UUID publicId,
        String customerNumber,
        String firstName,
        String lastName,
        String email,
        String phone,
        LocalDate dateOfBirth,
        String nationalId,
        String address,
        String city,
        String state,
        String country,
        String postalCode,
        CustomerStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
