package com.niyotechnologies.claimlens.organization.dto.request;


import com.niyotechnologies.claimlens.organization.enums.SubscriptionPlan;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateInsuranceCompanyRequest {

    @NotBlank
    private String name;

    @NotBlank(message = "Company code is mandatory")
    private String code;

    @NotBlank
    private String tenantKey;

    private String contactEmail;

    private String contactPhone;

    private String website;

    private String headOfficeAddress;

    private String logoUrl;

//    private String brandingConfig;

    @NotNull
    private SubscriptionPlan subscriptionPlan;

    @NotBlank
    private String currency;

    @NotBlank
    private String timezone;
}