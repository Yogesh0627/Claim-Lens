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
import com.niyotechnologies.claimlens.tenancy.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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


    /**
     * True when the caller holds the platform-admin authority.
     *
     * <p>{@link InsuranceCompany} is the tenant ROOT, so it deliberately extends {@code BaseEntity}
     * rather than {@code TenantAwareEntity} — otherwise you could never load a company before knowing
     * the tenant (chicken-and-egg at login). That means Hibernate's {@code @TenantId} discriminator
     * does NOT filter these queries, and this check is the ONLY thing standing between a tenant admin
     * and every other tenant's company row. Removing it re-opens a cross-tenant read AND write.
     */
    private boolean isPlatformAdmin() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null) {
            return false;
        }

        for (GrantedAuthority authority : auth.getAuthorities()) {

            if ("PLATFORM_ADMIN".equals(authority.getAuthority())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Rejects any attempt to touch a company other than the caller's own tenant.
     *
     * <p>Throws NOT_FOUND rather than FORBIDDEN on purpose: a 403 confirms the row exists, which
     * leaks the existence of other tenants. Same rule the organization endpoints follow.
     */
    private void assertOwnTenantOrPlatformAdmin(Long companyId) {

        if (isPlatformAdmin()) {
            return;
        }

        Long callerTenant = TenantContext.getTenantIdOrNull();

        if (callerTenant == null || !callerTenant.equals(companyId)) {

            throw new NotFoundException(
                    "COMPANY_NOT_FOUND",
                    "Insurance company not found"
            );
        }
    }


    @Override
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
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

        assertOwnTenantOrPlatformAdmin(companyId);

        InsuranceCompany company = getCompanyOrThrow(companyId);

        return insuranceCompanyMapper.toResponse(company);
    }

    /** The caller's own company, resolved from the JWT — never from a client-supplied id. */
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ORG_COMPANY_READ')")
    public InsuranceCompanyResponse getMyCompany() {

        return insuranceCompanyMapper.toResponse(
                getCompanyOrThrow(TenantContext.getTenantId())
        );
    }

    /** Cross-tenant listing — platform admins only. */
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
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

        assertOwnTenantOrPlatformAdmin(companyId);

        InsuranceCompany company =
                getCompanyOrThrow(companyId);

        insuranceCompanyMapper.updateEntity(company, request);

        InsuranceCompany updatedCompany =
                insuranceCompanyRepository.save(company);

        return insuranceCompanyMapper.toResponse(updatedCompany);

    }

    /** Retiring a tenant is a platform operation — a tenant admin must not delete their own company. */
    @Override
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
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