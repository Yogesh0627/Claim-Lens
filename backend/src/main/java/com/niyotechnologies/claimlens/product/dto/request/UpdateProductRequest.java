package com.niyotechnologies.claimlens.product.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Update a product's name/description and lifecycle status (ACTIVE | INACTIVE | RETIRED). */
public record UpdateProductRequest(
        @NotBlank String name,
        String description,
        @NotBlank String status
) {
}
