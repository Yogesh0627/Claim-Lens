package com.niyotechnologies.claimlens.organization.service.impl;

import com.niyotechnologies.claimlens.common.exception.BusinessException;
import com.niyotechnologies.claimlens.common.exception.NotFoundException;
import com.niyotechnologies.claimlens.organization.dto.request.CreateInsuranceCompanyRequest;
import com.niyotechnologies.claimlens.organization.dto.request.UpdateInsuranceCompanyRequest;
import com.niyotechnologies.claimlens.organization.dto.response.InsuranceCompanyResponse;
import com.niyotechnologies.claimlens.organization.entity.InsuranceCompany;
import com.niyotechnologies.claimlens.organization.enums.InsuranceCompanyStatus;
import com.niyotechnologies.claimlens.organization.repository.InsuranceCompanyRepository;
import com.niyotechnologies.claimlens.organization.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class InsuranceCompanyServiceImpl
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

        return mapToResponse(savedCompany);
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


    @Override
    @Transactional(readOnly = true)
    public InsuranceCompanyResponse getInsuranceCompanyById(
            Long companyId
    ) {

        InsuranceCompany company = getCompanyOrThrow(companyId);

        return mapToResponse(company);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InsuranceCompanyResponse> getAllCompanies(){

        List <InsuranceCompany> companies = insuranceCompanyRepository.findAll();
        return companies.stream()
                .map(this::mapToResponse)
                .toList();
    }

    private InsuranceCompany getCompanyOrThrow(
            Long companyId
    ) {

        return insuranceCompanyRepository.findByIdAndIsDeletedFalse(companyId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "COMPANY_NOT_FOUND",
                                "Insurance company not found"
                        )
                );
    }

    private InsuranceCompanyResponse mapToResponse(
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

    @Override
    public InsuranceCompanyResponse updateInsuranceCompany(
            Long companyId,
            UpdateInsuranceCompanyRequest request
    ) {

        InsuranceCompany company =
                getCompanyOrThrow(companyId);

        company.setName(request.getName());

        company.setContactEmail(
                request.getContactEmail()
        );

        company.setContactPhone(
                request.getContactPhone()
        );

        company.setWebsite(
                request.getWebsite()
        );

        company.setHeadOfficeAddress(
                request.getHeadOfficeAddress()
        );

        company.setSubscriptionPlan(
                request.getSubscriptionPlan()
        );

        company.setCurrency(
                request.getCurrency()
        );

        company.setTimezone(
                request.getTimezone()
        );

        InsuranceCompany updatedCompany =
                insuranceCompanyRepository.save(company);

        return mapToResponse(updatedCompany);
    }

    public void deleteInsuranceCompany(Long companyId){

        InsuranceCompany company =
                getCompanyOrThrow(companyId);

        company.setIsDeleted(true);

        company.setDeletedAt(
                Instant.now()
        );

        insuranceCompanyRepository.save(company);
    }
}