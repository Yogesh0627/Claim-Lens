package com.niyotechnologies.claimlens.customer.dto.request;

import com.niyotechnologies.claimlens.customer.enums.CustomerStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateCustomerRequest(
        @NotBlank(message = "First name is required")
        @Size(max = 100)
        String firstName,

        @Size(max = 100)
        String lastName,

        @Email(message = "Email must be valid")
        String email,

        @Size(max = 30)
        String phone,

        LocalDate dateOfBirth,

        @Size(max = 64)
        String nationalId,

        String address,
        @Size(max = 100) String city,
        @Size(max = 100) String state,
        @Size(max = 100) String country,
        @Size(max = 20) String postalCode,

        @NotNull(message = "Status is required")
        CustomerStatus status
) {
}
