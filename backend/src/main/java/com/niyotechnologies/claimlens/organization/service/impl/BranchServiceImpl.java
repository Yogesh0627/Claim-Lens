package com.niyotechnologies.claimlens.organization.service.impl;

import com.niyotechnologies.claimlens.common.exception.BusinessException;
import com.niyotechnologies.claimlens.common.exception.NotFoundException;
import com.niyotechnologies.claimlens.organization.dto.request.CreateBranchRequest;
import com.niyotechnologies.claimlens.organization.dto.request.UpdateBranchRequest;
import com.niyotechnologies.claimlens.organization.dto.response.BranchResponse;
import com.niyotechnologies.claimlens.organization.entity.Branch;
import com.niyotechnologies.claimlens.organization.entity.InsuranceCompany;
import com.niyotechnologies.claimlens.organization.entity.Region;
import com.niyotechnologies.claimlens.organization.mapper.BranchMapper;
import com.niyotechnologies.claimlens.organization.repository.BranchRepository;
import com.niyotechnologies.claimlens.organization.repository.InsuranceCompanyRepository;
import com.niyotechnologies.claimlens.organization.repository.RegionRepository;
import com.niyotechnologies.claimlens.organization.service.BranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;


@Service
@RequiredArgsConstructor
public class BranchServiceImpl implements BranchService {


    private final BranchRepository branchRepository;
    private final RegionRepository regionRepository;
    private final InsuranceCompanyRepository insuranceCompanyRepository;
    private final BranchMapper branchMapper;

    private InsuranceCompany getCompanyOrThrow(
            Long companyId
    ) {

        return insuranceCompanyRepository
                .findByIdAndIsDeletedFalse(companyId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "COMPANY_NOT_FOUND",
                                "Insurance company not found"
                        )
                );
    }

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

    private Region getRegionForCompanyOrThrow(
            Long companyId,
            Long regionId
    ) {

        Region region = getRegionOrThrow(regionId);

        if (!region.getTenantId().equals(companyId)) {
            throw new NotFoundException(
                    "REGION_NOT_FOUND",
                    "Region not found"
            );
        }

        return region;
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
    public BranchResponse createBranch(
            Long companyId,
            Long regionId,
            CreateBranchRequest request
    ) {

        getCompanyOrThrow(companyId);

        Region region =
                getRegionForCompanyOrThrow(
                        companyId,
                        regionId
                );

        boolean branchExists =
                branchRepository
                        .existsByTenantIdAndCodeAndIsDeletedFalse(
                                companyId,
                                request.getCode()
                        );

        if (branchExists) {
            throw new BusinessException(
                    "BRANCH_CODE_ALREADY_EXISTS",
                    "Branch code already exists"
            );
        }

        Branch branch = branchMapper.toEntity(
                companyId,
                region.getId(),
                request
        );

        Branch savedBranch =
                branchRepository.save(branch);

        return branchMapper.toResponse(savedBranch);
    }


    @Override
    public BranchResponse getBranch(
            Long companyId,
            Long regionId,
            Long branchId
    ) {

        getCompanyOrThrow(companyId);

        getRegionForCompanyOrThrow(
                companyId,
                regionId
        );

        Branch branch =
                getBranchForRegionOrThrow(
                        regionId,
                        branchId
                );

        return branchMapper.toResponse(branch);
    }


    @Override
    public List<BranchResponse> getBranchesByRegion(
            Long companyId,
            Long regionId
    ) {

        getCompanyOrThrow(companyId);

        getRegionForCompanyOrThrow(
                companyId,
                regionId
        );

        List<Branch> branches =
                branchRepository.findAllByRegionIdAndIsDeletedFalse(regionId);

        return branchMapper.toResponseList(branches);
    }

    @Override
    @Transactional
    public BranchResponse updateBranch(
            Long companyId,
            Long regionId,
            Long branchId,
            UpdateBranchRequest request
    ) {

        getCompanyOrThrow(companyId);

        getRegionForCompanyOrThrow(
                companyId,
                regionId
        );

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
    public void deleteBranch(
            Long companyId,
            Long regionId,
            Long branchId
    ) {

        getCompanyOrThrow(companyId);

        getRegionForCompanyOrThrow(
                companyId,
                regionId
        );

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
