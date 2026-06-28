package com.niyotechnologies.claimlens.organization.mapper;

import com.niyotechnologies.claimlens.organization.dto.request.CreateInsuranceCompanyRequest;
import com.niyotechnologies.claimlens.organization.dto.request.UpdateInsuranceCompanyRequest;
import com.niyotechnologies.claimlens.organization.dto.response.InsuranceCompanyResponse;
import com.niyotechnologies.claimlens.organization.entity.InsuranceCompany;
import com.niyotechnologies.claimlens.organization.enums.InsuranceCompanyStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InsuranceCompanyMapper {

    public InsuranceCompany toEntity(CreateInsuranceCompanyRequest request) {

        InsuranceCompany company = new InsuranceCompany();

        company.setName(request.getName().trim());
        company.setCode(request.getCode().trim());
        company.setTenantKey(request.getTenantKey().trim());

        company.setContactEmail(trim(request.getContactEmail()));
        company.setContactPhone(trim(request.getContactPhone()));

        company.setWebsite(trim(request.getWebsite()));
        company.setHeadOfficeAddress(trim(request.getHeadOfficeAddress()));
        company.setLogoUrl(trim(request.getLogoUrl()));

        company.setSubscriptionPlan(request.getSubscriptionPlan());
        company.setCurrency(request.getCurrency());
        company.setTimezone(request.getTimezone());

        company.setStatus(InsuranceCompanyStatus.ONBOARDING);

        return company;
    }

    public void updateEntity(
            InsuranceCompany company,
            UpdateInsuranceCompanyRequest request
    ) {

        company.setName(request.getName().trim());

        company.setContactEmail(trim(request.getContactEmail()));
        company.setContactPhone(trim(request.getContactPhone()));

        company.setWebsite(trim(request.getWebsite()));
        company.setHeadOfficeAddress(trim(request.getHeadOfficeAddress()));

        company.setSubscriptionPlan(request.getSubscriptionPlan());
        company.setCurrency(request.getCurrency());
        company.setTimezone(request.getTimezone());
    }

    public InsuranceCompanyResponse toResponse(
            InsuranceCompany company
    ) {

        return InsuranceCompanyResponse.builder()
                .id(company.getId())
                .name(company.getName())
                .code(company.getCode())
                .tenantKey(company.getTenantKey())
                .status(company.getStatus())
                .subscriptionPlan(company.getSubscriptionPlan())
                .currency(company.getCurrency())
                .timezone(company.getTimezone())
                .createdAt(company.getCreatedAt())
                .build();
    }

    public List<InsuranceCompanyResponse> toResponseList(
            List<InsuranceCompany> companies
    ) {

        return companies.stream()
                .map(this::toResponse)
                .toList();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}