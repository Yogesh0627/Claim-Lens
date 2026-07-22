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
@RequestMapping("${claimlens.api.base-path}/organizations")
public class DesignationController {

    private final DesignationService designationService;

    @PostMapping("/designations")
    public ApiResponse<DesignationResponse> createDesignation(
            @Valid @RequestBody CreateDesignationRequest request
    ) {

        return ApiResponse.success(
                designationService.createDesignation(request)
        );
    }

    @GetMapping("/designations/{designationId}")
    public ApiResponse<DesignationResponse> getDesignationById(
            @PathVariable Long designationId
    ) {

        return ApiResponse.success(
                designationService.getDesignationById(designationId)
        );
    }

    @GetMapping("/designations")
    public ApiResponse<List<DesignationResponse>> getAllDesignations() {

        return ApiResponse.success(
                designationService.getAllDesignations()
        );
    }

    @PutMapping("/designations/{designationId}")
    public ApiResponse<DesignationResponse> updateDesignation(
            @PathVariable Long designationId,
            @Valid @RequestBody UpdateDesignationRequest request
    ) {

        return ApiResponse.success(
                designationService.updateDesignation(
                        designationId,
                        request
                )
        );
    }

    @DeleteMapping("/designations/{designationId}")
    public ApiResponse<Void> deleteDesignation(
            @PathVariable Long designationId
    ) {

        designationService.deleteDesignation(designationId);

        return ApiResponse.success(null);
    }
}
