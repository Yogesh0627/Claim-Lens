package com.niyotechnologies.claimlens.organization.service;


import com.niyotechnologies.claimlens.organization.dto.request.CreateRegionRequest;
import com.niyotechnologies.claimlens.organization.dto.request.UpdateRegionRequest;
import com.niyotechnologies.claimlens.organization.dto.response.RegionResponse;

import java.util.List;

public interface RegionService {

    RegionResponse createRegion(CreateRegionRequest request);

    RegionResponse getRegion(Long regionId);

    List<RegionResponse> getRegions();

    RegionResponse updateRegion(Long regionId, UpdateRegionRequest request);

    void deleteRegion(Long regionId);
}
