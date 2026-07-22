package com.niyotechnologies.claimlens.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProductRequest(
        @NotBlank(message = "Product code is required")
        @Size(max = 50)
        String code,

        @NotBlank(message = "Product name is required")
        @Size(max = 255)
        String name,

        String description,

        @NotBlank(message = "Claim type code is required")
        @Size(max = 50)
        String claimTypeCode
) {
}
