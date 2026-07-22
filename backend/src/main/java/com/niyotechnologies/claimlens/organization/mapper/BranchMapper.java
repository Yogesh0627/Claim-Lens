package com.niyotechnologies.claimlens.organization.mapper;

import com.niyotechnologies.claimlens.organization.dto.request.CreateBranchRequest;
import com.niyotechnologies.claimlens.organization.dto.request.UpdateBranchRequest;
import com.niyotechnologies.claimlens.organization.dto.response.BranchResponse;
import com.niyotechnologies.claimlens.organization.entity.Branch;
import com.niyotechnologies.claimlens.organization.enums.BranchStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BranchMapper {

    public Branch toEntity(
            Long regionId,
            CreateBranchRequest request
    ) {

        Branch branch = new Branch();

        // tenant_id is populated by Hibernate @TenantId on persist — not set here.
        branch.setRegionId(regionId);

        branch.setCode(request.getCode().trim());
        branch.setName(request.getName().trim());

        branch.setOwnerUserId(request.getOwnerUserId());

        branch.setEmail(trim(request.getEmail()));
        branch.setPhone(trim(request.getPhone()));

        branch.setAddress(trim(request.getAddress()));
        branch.setCity(trim(request.getCity()));
        branch.setState(trim(request.getState()));
        branch.setCountry(trim(request.getCountry()));
        branch.setPostalCode(trim(request.getPostalCode()));

        branch.setStatus(BranchStatus.ACTIVE);

        branch.setDescription(trim(request.getDescription()));

        return branch;
    }

    public void updateEntity(
            Branch branch,
            UpdateBranchRequest request
    ) {

        branch.setName(request.getName().trim());

        branch.setOwnerUserId(request.getOwnerUserId());

        branch.setEmail(trim(request.getEmail()));
        branch.setPhone(trim(request.getPhone()));

        branch.setAddress(trim(request.getAddress()));
        branch.setCity(trim(request.getCity()));
        branch.setState(trim(request.getState()));
        branch.setCountry(trim(request.getCountry()));
        branch.setPostalCode(trim(request.getPostalCode()));

        branch.setStatus(request.getStatus());

        branch.setDescription(trim(request.getDescription()));
    }

    public BranchResponse toResponse(
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

    public List<BranchResponse> toResponseList(
            List<Branch> branches
    ) {

        return branches.stream()
                .map(this::toResponse)
                .toList();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}