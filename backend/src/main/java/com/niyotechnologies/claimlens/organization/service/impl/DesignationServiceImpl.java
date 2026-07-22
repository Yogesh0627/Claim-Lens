package com.niyotechnologies.claimlens.organization.service.impl;

import com.niyotechnologies.claimlens.common.exception.BusinessException;
import com.niyotechnologies.claimlens.common.exception.NotFoundException;
import com.niyotechnologies.claimlens.organization.dto.request.CreateDesignationRequest;
import com.niyotechnologies.claimlens.organization.dto.request.UpdateDesignationRequest;
import com.niyotechnologies.claimlens.organization.dto.response.DesignationResponse;
import com.niyotechnologies.claimlens.organization.entity.Designation;
import com.niyotechnologies.claimlens.organization.mapper.DesignationMapper;
import com.niyotechnologies.claimlens.organization.repository.DesignationRepository;
import com.niyotechnologies.claimlens.organization.service.DesignationService;
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
@Transactional
public class DesignationServiceImpl implements DesignationService {

    private final DesignationRepository designationRepository;
    private final DesignationMapper designationMapper;

    @Override
    @PreAuthorize("hasAuthority('ORG_DESIGNATION_WRITE')")
    public DesignationResponse createDesignation(
            CreateDesignationRequest request
    ) {

        validateDuplicateDesignationCode(request.getCode());

        validateDuplicateDesignationName(request.getName());

        // tenant_id is set by Hibernate @TenantId on persist — do not set it here.
        Designation designation = designationMapper.toEntity(request);

        Designation savedDesignation =
                designationRepository.save(designation);

        return designationMapper.toResponse(savedDesignation);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ORG_DESIGNATION_READ')")
    public DesignationResponse getDesignationById(
            Long designationId
    ) {

        Designation designation = getDesignationOrThrow(designationId);

        return designationMapper.toResponse(designation);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ORG_DESIGNATION_READ')")
    public List<DesignationResponse> getAllDesignations() {

        List<Designation> designations =
                designationRepository.findAllByIsDeletedFalse();

        return designationMapper.toResponseList(designations);
    }

    @Override
    @PreAuthorize("hasAuthority('ORG_DESIGNATION_WRITE')")
    public DesignationResponse updateDesignation(
            Long designationId,
            UpdateDesignationRequest request
    ) {

        Designation designation = getDesignationOrThrow(designationId);

        designationMapper.updateEntity(
                designation,
                request
        );

        Designation updatedDesignation =
                designationRepository.save(designation);

        return designationMapper.toResponse(updatedDesignation);
    }

    @Override
    @PreAuthorize("hasAuthority('ORG_DESIGNATION_WRITE')")
    public void deleteDesignation(
            Long designationId
    ) {

        Designation designation = getDesignationOrThrow(designationId);

        designation.setIsDeleted(true);
        designation.setDeletedAt(Instant.now());

        designationRepository.save(designation);
    }

    private Designation getDesignationOrThrow(Long designationId) {

        return designationRepository
                .findByIdAndIsDeletedFalse(designationId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "DESIGNATION_NOT_FOUND",
                                "Designation not found"
                        )
                );
    }

    private void validateDuplicateDesignationCode(
            String code
    ) {

        if (designationRepository.existsByCodeAndIsDeletedFalse(code)) {
            throw new BusinessException(
                    "DESIGNATION_CODE_ALREADY_EXISTS",
                    "Designation code already exists"
            );
        }
    }

    private void validateDuplicateDesignationName(
            String name
    ) {

        if (designationRepository.existsByNameAndIsDeletedFalse(name)) {
            throw new BusinessException(
                    "DESIGNATION_NAME_ALREADY_EXISTS",
                    "Designation name already exists"
            );
        }
    }
}
