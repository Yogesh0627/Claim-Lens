package com.niyotechnologies.claimlens.organization.service.impl;

import com.niyotechnologies.claimlens.common.exception.BusinessException;
import com.niyotechnologies.claimlens.common.exception.NotFoundException;
import com.niyotechnologies.claimlens.organization.dto.request.CreateInsuranceCompanyRequest;
import com.niyotechnologies.claimlens.organization.dto.request.UpdateInsuranceCompanyRequest;
import com.niyotechnologies.claimlens.organization.dto.response.InsuranceCompanyResponse;
import com.niyotechnologies.claimlens.organization.entity.InsuranceCompany;
import com.niyotechnologies.claimlens.organization.mapper.InsuranceCompanyMapper;
import com.niyotechnologies.claimlens.organization.repository.InsuranceCompanyRepository;
import com.niyotechnologies.claimlens.organization.service.InsuranceCompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class InsuranceCompanyServiceImpl
        implements InsuranceCompanyService {

    @Autowired
    private final InsuranceCompanyRepository insuranceCompanyRepository;

    @Autowired
    private final InsuranceCompanyMapper insuranceCompanyMapper;


    @Override
    @PreAuthorize("hasAuthority('ORG_COMPANY_WRITE')")
    public InsuranceCompanyResponse createInsuranceCompany(
            CreateInsuranceCompanyRequest request
    ) {

        validateCreateCompanyRequest(request);

        InsuranceCompany company =
                insuranceCompanyMapper.toEntity(request);

        InsuranceCompany saved =
                insuranceCompanyRepository.save(company);

        return insuranceCompanyMapper.toResponse(saved);
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
    @PreAuthorize("hasAuthority('ORG_COMPANY_READ')")
    public InsuranceCompanyResponse getInsuranceCompanyById(
            Long companyId
    ) {

        InsuranceCompany company = getCompanyOrThrow(companyId);

        return insuranceCompanyMapper.toResponse(company);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ORG_COMPANY_READ')")
    public List<InsuranceCompanyResponse> getAllCompanies(){

        List <InsuranceCompany> companies = insuranceCompanyRepository.findAllByIsDeletedFalse();
         return insuranceCompanyMapper.toResponseList(companies);
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


    @Override
    @PreAuthorize("hasAuthority('ORG_COMPANY_WRITE')")
    public InsuranceCompanyResponse updateInsuranceCompany(
            Long companyId,
            UpdateInsuranceCompanyRequest request
    ) {

        InsuranceCompany company =
                getCompanyOrThrow(companyId);

        insuranceCompanyMapper.updateEntity(company, request);

        InsuranceCompany updatedCompany =
                insuranceCompanyRepository.save(company);

        return insuranceCompanyMapper.toResponse(updatedCompany);

    }

    @Override
    @PreAuthorize("hasAuthority('ORG_COMPANY_WRITE')")
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