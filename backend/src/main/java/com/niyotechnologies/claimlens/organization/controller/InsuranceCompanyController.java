package com.niyotechnologies.claimlens.organization.controller;

import com.niyotechnologies.claimlens.common.response.ApiResponse;
import com.niyotechnologies.claimlens.organization.dto.request.CreateInsuranceCompanyRequest;
import com.niyotechnologies.claimlens.organization.dto.request.UpdateInsuranceCompanyRequest;
import com.niyotechnologies.claimlens.organization.dto.response.InsuranceCompanyResponse;
import com.niyotechnologies.claimlens.organization.service.InsuranceCompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${claimlens.api.base-path}/organizations")
@RequiredArgsConstructor
public class InsuranceCompanyController {

    @Autowired
    private final InsuranceCompanyService organizationService;

    @PostMapping("/companies")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InsuranceCompanyResponse> createCompany(
            @Valid @RequestBody CreateInsuranceCompanyRequest request
    ) {
        return ApiResponse.success(
                organizationService.createInsuranceCompany(request)
        );
    }

    /**
     * The caller's own company, resolved from the JWT.
     *
     * <p>Spring's pattern comparator prefers a literal segment over a variable, so this wins over
     * {@code /companies/{companyId}} regardless of declaration order. Without it, "/companies/me"
     * fell through to that route and failed to parse "me" as a Long — surfacing as a 500.
     */
    @GetMapping("/companies/me")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<InsuranceCompanyResponse> getMyCompany(){

        return ApiResponse.success(organizationService.getMyCompany());
    }

    @GetMapping("/companies/{companyId}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<InsuranceCompanyResponse> getCompany(@PathVariable Long companyId){

        InsuranceCompanyResponse company = organizationService.getInsuranceCompanyById(companyId);

        return ApiResponse.success(company);
    }

    @GetMapping("/companies")
    public ApiResponse<List<InsuranceCompanyResponse>>
    getAllCompanies() {

        return ApiResponse.success(
                organizationService.getAllCompanies()
        );
    }

    @PutMapping("/companies/{companyId}")
    public ApiResponse<InsuranceCompanyResponse> updateCompany(
            @PathVariable Long companyId,
            @Valid @RequestBody UpdateInsuranceCompanyRequest request
    ) {

        return ApiResponse.success(
                organizationService.updateInsuranceCompany(
                        companyId,
                        request
                )
        );
    }

    @DeleteMapping("/companies/{companyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCompany(
            @PathVariable Long companyId
    ) {
        organizationService
                .deleteInsuranceCompany(companyId);
    }
}