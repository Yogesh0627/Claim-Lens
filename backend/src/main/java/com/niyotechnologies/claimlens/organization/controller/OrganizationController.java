package com.niyotechnologies.claimlens.organization.controller;

import com.niyotechnologies.claimlens.common.response.ApiResponse;
import com.niyotechnologies.claimlens.organization.dto.request.CreateInsuranceCompanyRequest;
import com.niyotechnologies.claimlens.organization.dto.response.InsuranceCompanyResponse;
import com.niyotechnologies.claimlens.organization.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
//@RequestMapping("/api/v1/organizations")
@RequestMapping("${claimlens.api.base-path}/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    @Autowired
    private final OrganizationService organizationService;

    @PostMapping("/companies")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InsuranceCompanyResponse> createCompany(
            @Valid @RequestBody CreateInsuranceCompanyRequest request
    ) {
        return ApiResponse.success(
                organizationService.createInsuranceCompany(request)
        );
    }
}