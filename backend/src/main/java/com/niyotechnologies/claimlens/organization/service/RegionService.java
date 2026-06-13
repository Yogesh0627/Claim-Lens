package com.niyotechnologies.claimlens.organization.service;


import com.niyotechnologies.claimlens.organization.dto.request.CreateRegionRequest;
import com.niyotechnologies.claimlens.organization.dto.request.UpdateRegionRequest;
import com.niyotechnologies.claimlens.organization.dto.response.RegionResponse;

import java.util.List;

public interface RegionService {

    RegionResponse createRegion(
            Long tenantId,
            CreateRegionRequest request
    );

    RegionResponse getRegion(
            Long tenantId,
            Long regionId
    );

    List<RegionResponse> getRegionsByCompany(Long tenantId);

    RegionResponse updateRegion(
            Long tenantId,
            Long regionId,
            UpdateRegionRequest request
    );

    void deleteRegion(
            Long tenantId,
            Long regionId
    );
}
