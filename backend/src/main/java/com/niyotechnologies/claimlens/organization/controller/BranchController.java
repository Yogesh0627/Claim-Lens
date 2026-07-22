package com.niyotechnologies.claimlens.organization.controller;


import com.niyotechnologies.claimlens.common.response.ApiResponse;
import com.niyotechnologies.claimlens.organization.dto.request.CreateBranchRequest;
import com.niyotechnologies.claimlens.organization.dto.request.UpdateBranchRequest;
import com.niyotechnologies.claimlens.organization.dto.response.BranchResponse;
import com.niyotechnologies.claimlens.organization.service.BranchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${claimlens.api.base-path}/organizations")
@RequiredArgsConstructor
public class BranchController {

    @Autowired
    private final BranchService branchService;

    @PostMapping("/regions/{regionId}/branches")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BranchResponse> createBranch(
            @PathVariable Long regionId,
            @Valid @RequestBody CreateBranchRequest request
    ){

        return ApiResponse.success(
                branchService.createBranch(regionId, request)
        );
    }

    @GetMapping("/regions/{regionId}/branches/{branchId}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<BranchResponse> getBranch(@PathVariable Long regionId, @PathVariable Long branchId){

        BranchResponse branch = branchService.getBranch(regionId, branchId);

        return ApiResponse.success(branch);
    }


    @GetMapping("/regions/{regionId}/branches")
    public ApiResponse<List<BranchResponse>>
    getAllBranches(@PathVariable Long regionId) {

        return ApiResponse.success(
                branchService.getBranchesByRegion(regionId)
        );
    }

    @PutMapping("/regions/{regionId}/branches/{branchId}")
    public ApiResponse<BranchResponse> updateBranch(
            @PathVariable Long regionId,
            @PathVariable Long branchId,
            @Valid @RequestBody UpdateBranchRequest request
    ) {

        return ApiResponse.success(
                branchService.updateBranch(
                        regionId,
                        branchId,
                        request
                )
        );
    }

    @DeleteMapping("/regions/{regionId}/branches/{branchId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBranch(
            @PathVariable Long regionId,
            @PathVariable Long branchId
    ) {
        branchService
                .deleteBranch(regionId, branchId);
    }
}
