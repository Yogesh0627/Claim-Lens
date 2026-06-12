package com.niyotechnologies.claimlens.organization.service.impl;

import com.niyotechnologies.claimlens.common.exception.BusinessException;
import com.niyotechnologies.claimlens.organization.dto.request.CreateInsuranceCompanyRequest;
import com.niyotechnologies.claimlens.organization.dto.response.InsuranceCompanyResponse;
import com.niyotechnologies.claimlens.organization.entity.InsuranceCompany;
import com.niyotechnologies.claimlens.organization.enums.InsuranceCompanyStatus;
import com.niyotechnologies.claimlens.organization.repository.InsuranceCompanyRepository;
import com.niyotechnologies.claimlens.organization.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class OrganizationServiceImpl
        implements OrganizationService {

    @Autowired
    private final InsuranceCompanyRepository insuranceCompanyRepository;


    @Override
    public InsuranceCompanyResponse createInsuranceCompany(
            CreateInsuranceCompanyRequest request
    ) {

        validateCreateCompanyRequest(request);

        InsuranceCompany company = new InsuranceCompany();

        company.setName(request.getName());
        company.setCode(request.getCode());
        company.setTenantKey(request.getTenantKey());

        company.setContactEmail(request.getContactEmail());
        company.setContactPhone(request.getContactPhone());

        company.setWebsite(request.getWebsite());
        company.setHeadOfficeAddress(request.getHeadOfficeAddress());
        company.setLogoUrl(request.getLogoUrl());

//        company.setBrandingConfig(request.getBrandingConfig());

        company.setSubscriptionPlan(
                request.getSubscriptionPlan()
        );

        company.setCurrency(request.getCurrency());
        company.setTimezone(request.getTimezone());

        company.setStatus(
                InsuranceCompanyStatus.ONBOARDING
        );

        InsuranceCompany savedCompany =
                insuranceCompanyRepository.save(company);

        return InsuranceCompanyResponse.builder()
                .id(savedCompany.getId())
                .name(savedCompany.getName())
                .code(savedCompany.getCode())
                .tenantKey(savedCompany.getTenantKey())
                .status(savedCompany.getStatus())
                .subscriptionPlan(savedCompany.getSubscriptionPlan())
                .currency(savedCompany.getCurrency())
                .timezone(savedCompany.getTimezone())
                .createdAt(savedCompany.getCreatedAt())
                .build();
    }

    private void validateCreateCompanyRequest(
            CreateInsuranceCompanyRequest request
    ) {

        if (insuranceCompanyRepository.existsByCode(
                request.getCode()
        )) {

            throw new BusinessException(
                    "COMPANY_ALREADY_EXISTS",
                    "Insurance company code already exists"
            );
        }

        if (insuranceCompanyRepository.existsByTenantKey(
                request.getTenantKey()
        )) {

            throw new BusinessException(
                    "TENANT_KEY_ALREADY_EXISTS",
                    "Tenant key already exists"
            );
        }
    }
}