package com.niyotechnologies.claimlens.organization.service.impl;

import com.niyotechnologies.claimlens.common.exception.BusinessException;
import com.niyotechnologies.claimlens.common.exception.NotFoundException;
import com.niyotechnologies.claimlens.organization.dto.request.CreateBranchRequest;
import com.niyotechnologies.claimlens.organization.dto.request.UpdateBranchRequest;
import com.niyotechnologies.claimlens.organization.dto.response.BranchResponse;
import com.niyotechnologies.claimlens.organization.entity.Branch;
import com.niyotechnologies.claimlens.organization.entity.InsuranceCompany;
import com.niyotechnologies.claimlens.organization.entity.Region;
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

    private BranchResponse mapToResponse(
            Branch branch
    ) {

        return BranchResponse.builder()
                .id(branch.getId())
                .tenantId(branch.getTenantId())
                .regionId(branch.getRegionId())
                .code(branch.getCode())
                .name(branch.getName())
                .ownerUserId(branch.getOwnerUserId())
                .email(branch.getEmail())
                .phone(branch.getPhone())
                .address(branch.getAddress())
                .city(branch.getCity())
                .state(branch.getState())
                .country(branch.getCountry())
                .postalCode(branch.getPostalCode())
                .status(branch.getStatus())
                .description(branch.getDescription())
                .createdAt(branch.getCreatedAt())
                .updatedAt(branch.getUpdatedAt())
                .build();
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

        Branch branch = new Branch();

        branch.setTenantId(companyId);
        branch.setRegionId(region.getId());

        branch.setCode(request.getCode());
        branch.setName(request.getName());

        branch.setOwnerUserId(request.getOwnerUserId());

        branch.setEmail(request.getEmail());
        branch.setPhone(request.getPhone());

        branch.setAddress(request.getAddress());
        branch.setCity(request.getCity());
        branch.setState(request.getState());
        branch.setCountry(request.getCountry());
        branch.setPostalCode(request.getPostalCode());

        branch.setStatus(request.getStatus());
        branch.setDescription(request.getDescription());

        Branch savedBranch =
                branchRepository.save(branch);

        return mapToResponse(savedBranch);
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

        return mapToResponse(branch);
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

        return branchRepository
                .findAllByRegionIdAndIsDeletedFalse(regionId)
                .stream()
                .map(this::mapToResponse)
                .toList();
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

        branch.setName(request.getName());

        branch.setOwnerUserId(request.getOwnerUserId());

        branch.setEmail(request.getEmail());
        branch.setPhone(request.getPhone());

        branch.setAddress(request.getAddress());
        branch.setCity(request.getCity());
        branch.setState(request.getState());
        branch.setCountry(request.getCountry());
        branch.setPostalCode(request.getPostalCode());

        branch.setStatus(request.getStatus());
        branch.setDescription(request.getDescription());

        Branch updatedBranch =
                branchRepository.save(branch);

        return mapToResponse(updatedBranch);
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
