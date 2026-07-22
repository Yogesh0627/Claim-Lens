package com.niyotechnologies.claimlens.organization.service.impl;

import com.niyotechnologies.claimlens.common.exception.BusinessException;
import com.niyotechnologies.claimlens.common.exception.NotFoundException;
import com.niyotechnologies.claimlens.organization.dto.request.CreateRegionRequest;
import com.niyotechnologies.claimlens.organization.dto.request.UpdateRegionRequest;
import com.niyotechnologies.claimlens.organization.dto.response.RegionResponse;
import com.niyotechnologies.claimlens.organization.entity.Region;
import com.niyotechnologies.claimlens.organization.mapper.RegionMapper;
import com.niyotechnologies.claimlens.organization.repository.RegionRepository;
import com.niyotechnologies.claimlens.organization.service.RegionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Tenant scoping is enforced by Hibernate @TenantId (see TenantAwareEntity): every query and
 * insert is bound to the current tenant automatically, so this service no longer takes or checks
 * a companyId. The tenant comes from the JWT via TenantContext.
 */
@Service
@RequiredArgsConstructor
public class RegionServiceImpl implements RegionService {

    private final RegionRepository regionRepository;
    private final RegionMapper regionMapper;

    private Region getRegionOrThrow(Long regionId) {
        return regionRepository
                .findByIdAndIsDeletedFalse(regionId)
                .orElseThrow(() ->
                        new NotFoundException("REGION_NOT_FOUND", "Region not found"));
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ORG_REGION_WRITE')")
    public RegionResponse createRegion(CreateRegionRequest request) {

        boolean regionExists =
                regionRepository.existsByCodeAndIsDeletedFalse(request.getCode());

        if (regionExists) {
            throw new BusinessException(
                    "REGION_CODE_ALREADY_EXISTS",
                    "Region code already exists"
            );
        }

        // tenant_id is set by Hibernate @TenantId on persist — do not set it here.
        Region region = regionMapper.toEntity(request);

        return regionMapper.toResponse(regionRepository.save(region));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ORG_REGION_READ')")
    public RegionResponse getRegion(Long regionId) {
        return regionMapper.toResponse(getRegionOrThrow(regionId));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ORG_REGION_READ')")
    public List<RegionResponse> getRegions() {
        return regionMapper.toResponseList(regionRepository.findAllByIsDeletedFalse());
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ORG_REGION_WRITE')")
    public RegionResponse updateRegion(Long regionId, UpdateRegionRequest request) {

        Region region = getRegionOrThrow(regionId);

        regionMapper.updateEntity(region, request);

        return regionMapper.toResponse(regionRepository.save(region));
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ORG_REGION_WRITE')")
    public void deleteRegion(Long regionId) {

        Region region = getRegionOrThrow(regionId);

        region.setIsDeleted(true);
        region.setDeletedAt(Instant.now());

        regionRepository.save(region);
    }
}
