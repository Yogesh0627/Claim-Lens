package com.niyotechnologies.claimlens.organization.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateDepartmentRequest {

    @NotNull(message = "Tenant ID is required.")
    private Long tenantId;

    @NotBlank(message = "Department code is required.")
    @Size(max = 50, message = "Department code must not exceed 50 characters.")
    private String code;

    @NotBlank(message = "Department name is required.")
    @Size(max = 150, message = "Department name must not exceed 150 characters.")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters.")
    private String description;
}