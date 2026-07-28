package com.niyotechnologies.claimlens.organization.service.impl;

import com.niyotechnologies.claimlens.common.exception.BusinessException;
import com.niyotechnologies.claimlens.common.exception.NotFoundException;
import com.niyotechnologies.claimlens.organization.dto.request.CreateBranchRequest;
import com.niyotechnologies.claimlens.organization.dto.request.UpdateBranchRequest;
import com.niyotechnologies.claimlens.organization.dto.response.BranchResponse;
import com.niyotechnologies.claimlens.organization.entity.Branch;
import com.niyotechnologies.claimlens.organization.entity.Region;
import com.niyotechnologies.claimlens.organization.mapper.BranchMapper;
import com.niyotechnologies.claimlens.organization.repository.BranchRepository;
import com.niyotechnologies.claimlens.organization.repository.RegionRepository;
import com.niyotechnologies.claimlens.organization.service.BranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;


/**
 * Tenant scoping is enforced by Hibernate @TenantId (see TenantAwareEntity): every query and
 * insert is bound to the current tenant automatically, so this service no longer takes or checks
 * a companyId. The region parent (regionId) is a real FK and is still validated here. The tenant
 * comes from the JWT via TenantContext.
 */
@Service
@RequiredArgsConstructor
public class BranchServiceImpl implements BranchService {


    private final BranchRepository branchRepository;
    private final RegionRepository regionRepository;
    private final BranchMapper branchMapper;

    private Region getRegionOrThrow(
            Long regionId
    ) {

        return regionRepository
                .findByIdAndIsDeletedFalse(regionId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "REGION_NOT_FOUND",
                                "Region not found"
                        )
                );
    }


    private Branch getBranchOrThrow(
            Long branchId
    ) {

        return branchRepository
                .findByIdAndIsDeletedFalse(branchId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "BRANCH_NOT_FOUND",
                                "Branch not found"
                        )
                );
    }

    private Branch getBranchForRegionOrThrow(
            Long regionId,
            Long branchId
    ) {

        Branch branch = getBranchOrThrow(branchId);

        if (!branch.getRegionId().equals(regionId)) {
            throw new NotFoundException(
                    "BRANCH_NOT_FOUND",
                    "Branch not found"
            );
        }

        return branch;
    }


    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ORG_BRANCH_WRITE')")
    public BranchResponse createBranch(
            Long regionId,
            CreateBranchRequest request
    ) {

        Region region = getRegionOrThrow(regionId);

        boolean branchExists =
                branchRepository
                        .existsByCodeAndIsDeletedFalse(
                                request.getCode()
                        );

        if (branchExists) {
            throw new BusinessException(
                    "BRANCH_CODE_ALREADY_EXISTS",
                    "Branch code already exists"
            );
        }

        // tenant_id is set by Hibernate @TenantId on persist — do not set it here.
        // regionId is a real FK and is set from the validated region.
        Branch branch = branchMapper.toEntity(
                region.getId(),
                request
        );

        Branch savedBranch =
                branchRepository.save(branch);

        return branchMapper.toResponse(savedBranch);
    }


    @Override
    @PreAuthorize("hasAuthority('ORG_BRANCH_READ')")
    public BranchResponse getBranch(
            Long regionId,
            Long branchId
    ) {

        getRegionOrThrow(regionId);

        Branch branch =
                getBranchForRegionOrThrow(
                        regionId,
                        branchId
                );

        return branchMapper.toResponse(branch);
    }


    @Override
    @PreAuthorize("hasAuthority('ORG_BRANCH_READ')")
    public List<BranchResponse> getBranchesByRegion(
            Long regionId
    ) {

        getRegionOrThrow(regionId);

        List<Branch> branches =
                branchRepository.findAllByRegionIdAndIsDeletedFalse(regionId);

        return branchMapper.toResponseList(branches);
    }

    @Override
    @PreAuthorize("hasAuthority('ORG_BRANCH_READ')")
    public List<BranchResponse> getAllBranches() {
        return branchMapper.toResponseList(branchRepository.findAllByIsDeletedFalse());
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ORG_BRANCH_WRITE')")
    public BranchResponse updateBranch(
            Long regionId,
            Long branchId,
            UpdateBranchRequest request
    ) {

        getRegionOrThrow(regionId);

        Branch branch =
                getBranchForRegionOrThrow(
                        regionId,
                        branchId
                );

        branchMapper.updateEntity(
                branch,
                request
        );

        Branch updatedBranch =
                branchRepository.save(branch);

        return branchMapper.toResponse(updatedBranch);

    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ORG_BRANCH_WRITE')")
    public void deleteBranch(
            Long regionId,
            Long branchId
    ) {

        getRegionOrThrow(regionId);

        Branch branch =
                getBranchForRegionOrThrow(
                        regionId,
                        branchId
                );

        branch.setIsDeleted(true);
        branch.setDeletedAt(Instant.now());

        branchRepository.save(branch);
    }
}
