package com.niyotechnologies.claimlens.platform.service;

import com.niyotechnologies.claimlens.common.exception.BusinessException;
import com.niyotechnologies.claimlens.common.exception.NotFoundException;
import com.niyotechnologies.claimlens.organization.dto.response.InsuranceCompanyResponse;
import com.niyotechnologies.claimlens.organization.entity.InsuranceCompany;
import com.niyotechnologies.claimlens.organization.enums.InsuranceCompanyStatus;
import com.niyotechnologies.claimlens.organization.enums.SubscriptionPlan;
import com.niyotechnologies.claimlens.organization.repository.InsuranceCompanyRepository;
import com.niyotechnologies.claimlens.platform.dto.ImpersonationResponse;
import com.niyotechnologies.claimlens.platform.dto.OnboardTenantRequest;
import com.niyotechnologies.claimlens.role.repository.RoleRepository;
import com.niyotechnologies.claimlens.security.config.JwtProperties;
import com.niyotechnologies.claimlens.security.model.ClaimLensPrincipal;
import com.niyotechnologies.claimlens.security.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Tenant management for the platform admin: list/onboard/suspend insurance companies, and
 * impersonation — issuing a tenant-scoped token so the platform admin can enter a tenant's workspace
 * (as its admin) through the normal, @TenantId-protected app. InsuranceCompany is the tenant root
 * (global, no @TenantId), so it is read/written directly.
 */
@Service
@RequiredArgsConstructor
public class PlatformService {

    private static final String TENANT_ADMIN_ROLE = "TENANT_ADMIN";

    @Autowired
    private final InsuranceCompanyRepository companyRepository;
    @Autowired
    private final RoleRepository roleRepository;
    @Autowired
    private final JwtService jwtService;
    @Autowired
    private final JwtProperties jwtProperties;

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public List<InsuranceCompanyResponse> listTenants() {
        return companyRepository.findAllByIsDeletedFalse().stream().map(PlatformService::toResponse).toList();
    }

    @Transactional
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public InsuranceCompanyResponse onboard(OnboardTenantRequest request) {
        companyRepository.findByCode(request.code()).ifPresent(c -> {
            throw new BusinessException("TENANT_CODE_EXISTS", "A tenant with this code already exists");
        });
        companyRepository.findByTenantKey(request.tenantKey()).ifPresent(c -> {
            throw new BusinessException("TENANT_KEY_EXISTS", "A tenant with this key already exists");
        });

        InsuranceCompany company = new InsuranceCompany();
        company.setName(request.name());
        company.setCode(request.code());
        company.setTenantKey(request.tenantKey());
        company.setStatus(InsuranceCompanyStatus.ONBOARDING);
        company.setSubscriptionPlan(parsePlan(request.subscriptionPlan()));
        company.setCurrency(request.currency());
        company.setTimezone(request.timezone());
        company.setContactEmail(request.contactEmail());
        return toResponse(companyRepository.save(company));
    }

    @Transactional
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public InsuranceCompanyResponse setStatus(Long tenantId, String status) {
        InsuranceCompany company = companyRepository.findByIdAndIsDeletedFalse(tenantId)
                .orElseThrow(() -> new NotFoundException("TENANT_NOT_FOUND", "Tenant not found"));
        company.setStatus(parseStatus(status));
        return toResponse(companyRepository.save(company));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public ImpersonationResponse impersonate(Long tenantId, ClaimLensPrincipal platformAdmin) {
        InsuranceCompany company = companyRepository.findByIdAndIsDeletedFalse(tenantId)
                .orElseThrow(() -> new NotFoundException("TENANT_NOT_FOUND", "Tenant not found"));
        Long tenantAdminRoleId = roleRepository.findByCodeAndIsDeletedFalse(TENANT_ADMIN_ROLE)
                .orElseThrow(() -> new BusinessException("ROLE_MISSING", "TENANT_ADMIN role not found"))
                .getId();

        // Tenant-scoped token: same platform-admin identity (for audit), but tid=target tenant and the
        // TENANT_ADMIN role, so all @TenantId + @PreAuthorize endpoints work for that tenant.
        ClaimLensPrincipal impersonated = new ClaimLensPrincipal(
                platformAdmin.userId(), tenantId, tenantAdminRoleId,
                platformAdmin.email(), platformAdmin.employeeCode(), null);
        String accessToken = jwtService.generateToken(impersonated);

        return new ImpersonationResponse(
                accessToken, "Bearer", jwtProperties.accessTokenTtl().getSeconds(),
                tenantId, company.getName());
    }

    private SubscriptionPlan parsePlan(String plan) {
        if (plan == null || plan.isBlank()) {
            return SubscriptionPlan.BASIC;
        }
        try {
            return SubscriptionPlan.valueOf(plan.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_PLAN", "Unknown subscription plan: " + plan);
        }
    }

    private InsuranceCompanyStatus parseStatus(String status) {
        try {
            return InsuranceCompanyStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_STATUS", "Unknown tenant status: " + status);
        }
    }

    private static InsuranceCompanyResponse toResponse(InsuranceCompany c) {
        return InsuranceCompanyResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .code(c.getCode())
                .tenantKey(c.getTenantKey())
                .status(c.getStatus())
                .subscriptionPlan(c.getSubscriptionPlan())
                .currency(c.getCurrency())
                .timezone(c.getTimezone())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
