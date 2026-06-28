package com.niyotechnologies.claimlens.organization.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateDesignationRequest {

    @NotBlank(message = "Designation name is required.")
    @Size(max = 150)
    private String name;

    @Size(max = 1000)
    private String description;

}