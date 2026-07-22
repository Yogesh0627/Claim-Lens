package com.niyotechnologies.claimlens.platform.controller;

import com.niyotechnologies.claimlens.common.response.ApiResponse;
import com.niyotechnologies.claimlens.organization.dto.response.InsuranceCompanyResponse;
import com.niyotechnologies.claimlens.platform.dto.ImpersonationResponse;
import com.niyotechnologies.claimlens.platform.dto.OnboardTenantRequest;
import com.niyotechnologies.claimlens.platform.dto.PlatformAnalyticsResponse;
import com.niyotechnologies.claimlens.platform.dto.TenantStatusRequest;
import com.niyotechnologies.claimlens.platform.service.PlatformAnalyticsService;
import com.niyotechnologies.claimlens.platform.service.PlatformService;
import com.niyotechnologies.claimlens.security.model.ClaimLensPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Cross-tenant platform administration. All methods gated by PLATFORM_ADMIN on the service layer. */
@RestController
@RequestMapping("${claimlens.api.base-path}/platform")
@RequiredArgsConstructor
public class PlatformController {

    @Autowired
    private final PlatformAnalyticsService analyticsService;
    @Autowired
    private final PlatformService platformService;

    @GetMapping("/analytics")
    public ApiResponse<PlatformAnalyticsResponse> analytics() {
        return ApiResponse.success(analyticsService.analytics());
    }

    @GetMapping("/tenants")
    public ApiResponse<List<InsuranceCompanyResponse>> tenants() {
        return ApiResponse.success(platformService.listTenants());
    }

    @PostMapping("/tenants")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InsuranceCompanyResponse> onboard(@Valid @RequestBody OnboardTenantRequest request) {
        return ApiResponse.success(platformService.onboard(request));
    }

    @PostMapping("/tenants/{tenantId}/status")
    public ApiResponse<InsuranceCompanyResponse> setStatus(
            @PathVariable Long tenantId,
            @Valid @RequestBody TenantStatusRequest request) {
        return ApiResponse.success(platformService.setStatus(tenantId, request.status()));
    }

    @PostMapping("/tenants/{tenantId}/impersonate")
    public ApiResponse<ImpersonationResponse> impersonate(
            @PathVariable Long tenantId,
            @AuthenticationPrincipal ClaimLensPrincipal principal) {
        return ApiResponse.success(platformService.impersonate(tenantId, principal));
    }
}
