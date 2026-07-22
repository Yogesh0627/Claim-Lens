package com.niyotechnologies.claimlens.organization.service;

import com.niyotechnologies.claimlens.organization.dto.request.CreateBranchRequest;
import com.niyotechnologies.claimlens.organization.dto.request.UpdateBranchRequest;
import com.niyotechnologies.claimlens.organization.dto.response.BranchResponse;

import java.util.List;

public interface BranchService {

    BranchResponse createBranch(
            Long regionId,
            CreateBranchRequest request
    );

    BranchResponse getBranch(
            Long regionId,
            Long branchId
    );

    List<BranchResponse> getBranchesByRegion(
            Long regionId
    );

    BranchResponse updateBranch(
            Long regionId,
            Long branchId,
            UpdateBranchRequest request
    );

    void deleteBranch(
            Long regionId,
            Long branchId
    );
}
