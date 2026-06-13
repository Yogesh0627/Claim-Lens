package com.niyotechnologies.claimlens.organization.dto.request;


import com.niyotechnologies.claimlens.organization.enums.RegionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateRegionRequest {

    @NotBlank(message = "Region name is required")
    @Size(max = 255, message = "Region name cannot exceed 255 characters")
    private String name;

    private Long ownerUserId;

    @NotNull(message = "Region status is required")
    private RegionStatus status;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;
}