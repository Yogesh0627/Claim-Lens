package com.niyotechnologies.claimlens.organization.controller;


import com.niyotechnologies.claimlens.common.response.ApiResponse;
import com.niyotechnologies.claimlens.organization.dto.request.CreateRegionRequest;
import com.niyotechnologies.claimlens.organization.dto.request.UpdateRegionRequest;
import com.niyotechnologies.claimlens.organization.dto.response.RegionResponse;
import com.niyotechnologies.claimlens.organization.service.RegionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
//@RequestMapping("/api/v1/organizations")
@RequestMapping("${claimlens.api.base-path}/organizations/companies")
@RequiredArgsConstructor
public class RegionController {


    @Autowired
    private final RegionService regionService;

    @PostMapping("/{companyId}/regions")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RegionResponse> createRegion(
            @PathVariable Long companyId,
            @Valid @RequestBody CreateRegionRequest request
    ) {
        return ApiResponse.success(
                regionService.createRegion(companyId,request)
        );
    }

    @GetMapping("/{companyId}/regions/{regionId}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<RegionResponse> getRegion(@PathVariable Long companyId, @PathVariable Long regionId){

        RegionResponse region = regionService.getRegion(companyId, regionId);

        return ApiResponse.success(region);
    }

    @GetMapping("/{companyId}/regions")
    public ApiResponse<List<RegionResponse>>
    getAllRegions(@PathVariable Long companyId) {

        return ApiResponse.success(
                regionService.getRegionsByCompany(companyId)
        );
    }

    @PutMapping("/{companyId}/regions/{regionId}")
    public ApiResponse<RegionResponse> updateRegion(
            @PathVariable Long companyId,
            @PathVariable Long regionId,
            @Valid @RequestBody UpdateRegionRequest request
    ) {

        return ApiResponse.success(
                regionService.updateRegion(
                        companyId,
                        regionId,
                        request
                )
        );
    }

    @DeleteMapping("/{companyId}/regions/{regionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRegion(
            @PathVariable Long companyId,
            @PathVariable Long regionId
    ) {
        regionService
                .deleteRegion(companyId, regionId);
    }
}
