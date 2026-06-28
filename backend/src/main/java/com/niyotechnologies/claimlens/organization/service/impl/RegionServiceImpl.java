package com.niyotechnologies.claimlens.organization.service.impl;

import com.niyotechnologies.claimlens.common.exception.BusinessException;
import com.niyotechnologies.claimlens.common.exception.NotFoundException;
import com.niyotechnologies.claimlens.organization.dto.request.CreateRegionRequest;
import com.niyotechnologies.claimlens.organization.dto.request.UpdateRegionRequest;
import com.niyotechnologies.claimlens.organization.dto.response.RegionResponse;
import com.niyotechnologies.claimlens.organization.entity.InsuranceCompany;
import com.niyotechnologies.claimlens.organization.entity.Region;
import com.niyotechnologies.claimlens.organization.mapper.RegionMapper;
import com.niyotechnologies.claimlens.organization.repository.InsuranceCompanyRepository;
import com.niyotechnologies.claimlens.organization.repository.RegionRepository;
import com.niyotechnologies.claimlens.organization.service.RegionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegionServiceImpl implements RegionService {

    private final RegionRepository regionRepository;
    private final InsuranceCompanyRepository insuranceCompanyRepository;
    private final RegionMapper regionMapper;

    private InsuranceCompany getCompanyOrThrow(
            Long companyId
    ) {

        return insuranceCompanyRepository.findByIdAndIsDeletedFalse(companyId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "COMPANY_NOT_FOUND",
                                "Insurance company not found"
                        )
                );
    }

    private Region getRegionOrThrow(Long regionId) {
        return regionRepository
                .findByIdAndIsDeletedFalse(regionId)
                .orElseThrow(() ->
                        new NotFoundException("REGION_NOT_FOUND","Region not found"));
    }

    private RegionResponse mapToResponse(Region region) {
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


    @Override
    @Transactional
    public RegionResponse createRegion(
            Long companyId,
            CreateRegionRequest request
    ) {

        getCompanyOrThrow(companyId);

        boolean regionExists =
                regionRepository
                        .existsByTenantIdAndCodeAndIsDeletedFalse(
                                companyId,
                                request.getCode()
                        );

        if (regionExists) {
            throw new BusinessException(
                    "REGION_CODE_ALREADY_EXISTS",
                    "Region code already exists"
            );
        }

        Region region =
                regionMapper.toEntity(companyId, request);

        Region savedRegion =
                regionRepository.save(region);

        return regionMapper.toResponse(savedRegion);
    }


    @Override
    public RegionResponse getRegion(
            Long companyId,
            Long regionId
    ) {

        getCompanyOrThrow(companyId);
        Region region = getRegionForCompanyOrThrow(
                        companyId,
                        regionId);

        return regionMapper.toResponse(region);
    }


    @Override
    @Transactional(readOnly = true)
    public List<RegionResponse> getRegionsByCompany(
            Long companyId
    ) {

        getCompanyOrThrow(companyId);

        List<Region> regions =
                regionRepository.findAllByTenantIdAndIsDeletedFalse(companyId);

        return regionMapper.toResponseList(regions);
    }

    @Override
    @Transactional
    public RegionResponse updateRegion(
            Long companyId,
            Long regionId,
            UpdateRegionRequest request
    ) {

        getCompanyOrThrow(companyId);

        Region region =
                getRegionForCompanyOrThrow(
                        companyId,
                        regionId
                );

        regionMapper.updateEntity(
                region,
                request
        );

        Region updatedRegion =
                regionRepository.save(region);

        return regionMapper.toResponse(updatedRegion);
    }


    @Override
    @Transactional
    public void deleteRegion(
            Long companyId,
            Long regionId
    ) {

        getCompanyOrThrow(companyId);

        Region region =
                getRegionForCompanyOrThrow(
                        companyId,
                        regionId
                );

        region.setIsDeleted(true);
        region.setDeletedAt(Instant.now());

        regionRepository.save(region);
    }
}
