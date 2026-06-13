package com.niyotechnologies.claimlens.organization.dto.request;

import com.niyotechnologies.claimlens.organization.enums.SubscriptionPlan;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateInsuranceCompanyRequest {

    @NotBlank(message = "Company name is required")
    private String name;

    private String contactEmail;

    private String contactPhone;

    private String website;

    private String headOfficeAddress;

    @NotBlank(message = "Subscription plan is required")
    private SubscriptionPlan subscriptionPlan;

    @NotBlank(message = "Currency is required")
    private String currency;

    @NotBlank(message = "Timezone is required")
    private String timezone;
}