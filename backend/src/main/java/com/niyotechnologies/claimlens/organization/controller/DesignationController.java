package com.niyotechnologies.claimlens.organization.controller;

import com.niyotechnologies.claimlens.common.response.ApiResponse;
import com.niyotechnologies.claimlens.organization.dto.request.CreateDesignationRequest;
import com.niyotechnologies.claimlens.organization.dto.request.UpdateDesignationRequest;
import com.niyotechnologies.claimlens.organization.dto.response.DesignationResponse;
import com.niyotechnologies.claimlens.organization.service.DesignationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("${claimlens.api.base-path}/organizations/companies/{companyId}/designations")
public class DesignationController {

    private final DesignationService designationService;

    @PostMapping
    public ApiResponse<DesignationResponse> createDesignation(
            @PathVariable Long companyId,
            @Valid @RequestBody CreateDesignationRequest request
    ) {

        return ApiResponse.success(
                designationService.createDesignation(
                        companyId,
                        request
                )
        );
    }

    @GetMapping("/{designationId}")
    public ApiResponse<DesignationResponse> getDesignationById(
            @PathVariable Long companyId,
            @PathVariable Long designationId
    ) {

        return ApiResponse.success(
                designationService.getDesignationById(
                        companyId,
                        designationId
                )
        );
    }

    @GetMapping
    public ApiResponse<List<DesignationResponse>> getAllDesignations(
            @PathVariable Long companyId
    ) {

        return ApiResponse.success(
                designationService.getAllDesignations(companyId)
        );
    }

    @PutMapping("/{designationId}")
    public ApiResponse<DesignationResponse> updateDesignation(
            @PathVariable Long companyId,
            @PathVariable Long designationId,
            @Valid @RequestBody UpdateDesignationRequest request
    ) {

        return ApiResponse.success(
                designationService.updateDesignation(
                        companyId,
                        designationId,
                        request
                )
        );
    }

    @DeleteMapping("/{designationId}")
    public ApiResponse<Void> deleteDesignation(
            @PathVariable Long companyId,
            @PathVariable Long designationId
    ) {

        designationService.deleteDesignation(
                companyId,
                designationId
        );

        return ApiResponse.success(null);
    }
}