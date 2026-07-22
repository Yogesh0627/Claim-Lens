package com.niyotechnologies.claimlens.organization.mapper;

import com.niyotechnologies.claimlens.organization.dto.request.CreateRegionRequest;
import com.niyotechnologies.claimlens.organization.dto.request.UpdateRegionRequest;
import com.niyotechnologies.claimlens.organization.dto.response.RegionResponse;
import com.niyotechnologies.claimlens.organization.entity.Region;
import com.niyotechnologies.claimlens.organization.enums.RegionStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RegionMapper {

    public Region toEntity(CreateRegionRequest request) {

        Region region = new Region();

        // tenant_id is populated by Hibernate @TenantId on persist — not set here.
        region.setCode(request.getCode().trim());
        region.setName(request.getName().trim());

        region.setOwnerUserId(request.getOwnerUserId());

        region.setStatus(RegionStatus.ACTIVE);

        region.setDescription(trim(request.getDescription()));

        return region;
    }

    public void updateEntity(
            Region region,
            UpdateRegionRequest request
    ) {

        region.setName(request.getName().trim());

        region.setOwnerUserId(request.getOwnerUserId());

        region.setStatus(request.getStatus());

        region.setDescription(trim(request.getDescription()));
    }

    public RegionResponse toResponse(
            Region region
    ) {

        return RegionResponse.builder()
                .id(region.getId())
                .tenantId(region.getTenantId())
                .code(region.getCode())
                .name(region.getName())
                .ownerUserId(region.getOwnerUserId())
                .status(region.getStatus())
                .description(region.getDescription())
                .createdAt(region.getCreatedAt())
                .updatedAt(region.getUpdatedAt())
                .build();
    }

    public List<RegionResponse> toResponseList(
            List<Region> regions
    ) {

        return regions.stream()
                .map(this::toResponse)
                .toList();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}