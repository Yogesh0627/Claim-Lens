package com.niyotechnologies.claimlens.organization.service.impl;

import com.niyotechnologies.claimlens.common.exception.BusinessException;
import com.niyotechnologies.claimlens.common.exception.NotFoundException;
import com.niyotechnologies.claimlens.organization.dto.request.CreateInsuranceCompanyRequest;
import com.niyotechnologies.claimlens.organization.dto.request.UpdateInsuranceCompanyRequest;
import com.niyotechnologies.claimlens.organization.dto.response.InsuranceCompanyResponse;
import com.niyotechnologies.claimlens.organization.entity.InsuranceCompany;
import com.niyotechnologies.claimlens.organization.enums.InsuranceCompanyStatus;
import com.niyotechnologies.claimlens.organization.mapper.InsuranceCompanyMapper;
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

    @Autowired
    private final InsuranceCompanyMapper insuranceCompanyMapper;


    @Override
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
    public InsuranceCompanyResponse getInsuranceCompanyById(
            Long companyId
    ) {

        InsuranceCompany company = getCompanyOrThrow(companyId);

        return insuranceCompanyMapper.toResponse(company);
    }

    @Override
    @Transactional(readOnly = true)
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
    public InsuranceCompanyResponse updateInsuranceCompany(
            Long companyId,
            UpdateInsuranceCompanyRequest request
    ) {

        InsuranceCompany company =
                getCompanyOrThrow(companyId);

        InsuranceCompany updatedCompany =
                insuranceCompanyRepository.save(company);

        return insuranceCompanyMapper.toResponse(updatedCompany);

    }

    @Override
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