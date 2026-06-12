package com.niyotechnologies.claimlens.organization.entity;

import com.niyotechnologies.claimlens.common.entity.BaseEntity;
import com.niyotechnologies.claimlens.organization.enums.InsuranceCompanyStatus;
import com.niyotechnologies.claimlens.organization.enums.SubscriptionPlan;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "insurance_company")
@Getter
@Setter
public class InsuranceCompany extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 50, unique = true)
    private String code;

    @Column(name = "tenant_key", nullable = false, length = 100, unique = true)
    private String tenantKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private InsuranceCompanyStatus status;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone")
    private String contactPhone;

    private String website;

    @Column(name = "head_office_address")
    private String headOfficeAddress;

    @Column(name = "logo_url")
    private String logoUrl;

//    @Column(name = "branding_config", columnDefinition = "jsonb")
//    private String brandingConfig;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_plan", nullable = false, length = 50)
    private SubscriptionPlan subscriptionPlan;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(nullable = false, length = 100)
    private String timezone;
}