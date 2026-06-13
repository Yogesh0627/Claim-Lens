package com.niyotechnologies.claimlens.organization.controller;


import com.niyotechnologies.claimlens.common.response.ApiResponse;
import com.niyotechnologies.claimlens.organization.dto.request.CreateBranchRequest;
import com.niyotechnologies.claimlens.organization.dto.request.UpdateBranchRequest;
import com.niyotechnologies.claimlens.organization.dto.request.UpdateRegionRequest;
import com.niyotechnologies.claimlens.organization.dto.response.BranchResponse;
import com.niyotechnologies.claimlens.organization.dto.response.RegionResponse;
import com.niyotechnologies.claimlens.organization.service.BranchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
//@RequestMapping("/api/v1/organizations")
@RequestMapping("${claimlens.api.base-path}/organizations/companies")
@RequiredArgsConstructor
public class BranchController {

    @Autowired
    private final BranchService branchService;

    @PostMapping("/{companyId}/regions/{regionId}/branches")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BranchResponse> createBranch(
            @PathVariable Long companyId,
            @PathVariable Long regionId,
            @Valid @RequestBody CreateBranchRequest request
    ){

        return ApiResponse.success(
                branchService.createBranch(companyId,regionId,request)
        );
    }

    @GetMapping("/{companyId}/regions/{regionId}/branches/{branchId}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<BranchResponse> getBranch(@PathVariable Long companyId, @PathVariable Long regionId, @PathVariable Long branchId){

        BranchResponse branch = branchService.getBranch(companyId, regionId, branchId);

        return ApiResponse.success(branch);
    }


    @GetMapping("/{companyId}/regions/{regionId}/branches")
    public ApiResponse<List<BranchResponse>>
    getAllBranches(@PathVariable Long companyId, @PathVariable Long regionId) {

        return ApiResponse.success(
                branchService.getBranchesByRegion(companyId, regionId)
        );
    }

    @PutMapping("/{companyId}/regions/{regionId}/branches/{branchId}")
    public ApiResponse<BranchResponse> updateBranch(
            @PathVariable Long companyId,
            @PathVariable Long regionId,
            @PathVariable Long branchId,
            @Valid @RequestBody UpdateBranchRequest request
    ) {

        return ApiResponse.success(
                branchService.updateBranch(
                        companyId,
                        regionId,
                        branchId,
                        request
                )
        );
    }

    @DeleteMapping("/{companyId}/regions/{regionId}/branches/{branchId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBranch(
            @PathVariable Long companyId,
            @PathVariable Long regionId,
            @PathVariable Long branchId
    ) {
        branchService
                .deleteBranch(companyId, regionId, branchId);
    }
}
