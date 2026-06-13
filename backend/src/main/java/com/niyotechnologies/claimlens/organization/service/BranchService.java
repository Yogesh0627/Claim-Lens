package com.niyotechnologies.claimlens.organization.service;

import com.niyotechnologies.claimlens.organization.dto.request.CreateBranchRequest;
import com.niyotechnologies.claimlens.organization.dto.request.UpdateBranchRequest;
import com.niyotechnologies.claimlens.organization.dto.response.BranchResponse;

import java.util.List;

public interface BranchService {

    BranchResponse createBranch(
            Long companyId,
            Long regionId,
            CreateBranchRequest request
    );

    BranchResponse getBranch(
            Long companyId,
            Long regionId,
            Long branchId
    );

    List<BranchResponse> getBranchesByRegion(
            Long companyId,
            Long regionId
    );

    BranchResponse updateBranch(
            Long companyId,
            Long regionId,
            Long branchId,
            UpdateBranchRequest request
    );

    void deleteBranch(
            Long companyId,
            Long regionId,
            Long branchId
    );
}