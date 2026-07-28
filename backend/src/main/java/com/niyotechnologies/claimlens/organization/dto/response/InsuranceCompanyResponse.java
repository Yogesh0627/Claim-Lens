package com.niyotechnologies.claimlens.organization.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.niyotechnologies.claimlens.organization.enums.InsuranceCompanyStatus;
import com.niyotechnologies.claimlens.organization.enums.SubscriptionPlan;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class InsuranceCompanyResponse {

    private Long id;

    private String name;

    private String code;

    private String tenantKey;

    private InsuranceCompanyStatus status;

    private SubscriptionPlan subscriptionPlan;

    private String currency;

    private String timezone;

    private String contactEmail;

    private Instant createdAt;

    // Boxed Boolean (not primitive): Lombok then generates getIsDeleted() rather than isDeleted(), so
    // Jackson serializes the single property "isDeleted". A primitive boolean here would serialize as
    // "deleted" (Jackson strips the "is" from the isDeleted() getter), breaking the client contract.
    // @JsonProperty pins the name belt-and-braces.
    @JsonProperty("isDeleted")
    private Boolean isDeleted;

    private Instant deletedAt;
}