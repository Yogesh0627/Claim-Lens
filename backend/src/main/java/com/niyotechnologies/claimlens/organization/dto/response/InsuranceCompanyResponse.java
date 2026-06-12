package com.niyotechnologies.claimlens.organization.dto.response;

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

    private Instant createdAt;
}