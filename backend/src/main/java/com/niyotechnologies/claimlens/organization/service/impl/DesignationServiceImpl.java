package com.niyotechnologies.claimlens.organization.service.impl;

import com.niyotechnologies.claimlens.common.exception.BusinessException;
import com.niyotechnologies.claimlens.common.exception.NotFoundException;
import com.niyotechnologies.claimlens.organization.dto.request.CreateDesignationRequest;
import com.niyotechnologies.claimlens.organization.dto.request.UpdateDesignationRequest;
import com.niyotechnologies.claimlens.organization.dto.response.DesignationResponse;
import com.niyotechnologies.claimlens.organization.entity.Designation;
import com.niyotechnologies.claimlens.organization.entity.InsuranceCompany;
import com.niyotechnologies.claimlens.organization.mapper.DesignationMapper;
import com.niyotechnologies.claimlens.organization.repository.DesignationRepository;
import com.niyotechnologies.claimlens.organization.repository.InsuranceCompanyRepository;
import com.niyotechnologies.claimlens.organization.service.DesignationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DesignationServiceImpl implements DesignationService {

    private final DesignationRepository designationRepository;
    private final InsuranceCompanyRepository insuranceCompanyRepository;
    private final DesignationMapper designationMapper;

    @Override
    public DesignationResponse createDesignation(
            Long companyId,
            CreateDesignationRequest request
    ) {

        getCompanyOrThrow(companyId);

        validateDuplicateDesignationCode(
                companyId,
                request.getCode()
        );

        validateDuplicateDesignationName(
                companyId,
                request.getName()
        );

        Designation designation =
                designationMapper.toEntity(
                        companyId,
                        request
                );

        Designation savedDesignation =
                designationRepository.save(designation);

        return designationMapper.toResponse(savedDesignation);
    }

    @Override
    @Transactional(readOnly = true)
    public DesignationResponse getDesignationById(
            Long companyId,
            Long designationId
    ) {

        getCompanyOrThrow(companyId);

        Designation designation =
                getDesignationForCompanyOrThrow(
                        companyId,
                        designationId
                );

        return designationMapper.toResponse(designation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DesignationResponse> getAllDesignations(
            Long companyId
    ) {

        getCompanyOrThrow(companyId);

        List<Designation> designations =
                designationRepository.findAllByTenantIdAndIsDeletedFalse(companyId);

        return designationMapper.toResponseList(designations);
    }

    @Override
    public DesignationResponse updateDesignation(
            Long companyId,
            Long designationId,
            UpdateDesignationRequest request
    ) {

        getCompanyOrThrow(companyId);

        Designation designation =
                getDesignationForCompanyOrThrow(
                        companyId,
                        designationId
                );

        designationMapper.updateEntity(
                designation,
                request
        );

        Designation updatedDesignation =
                designationRepository.save(designation);

        return designationMapper.toResponse(updatedDesignation);
    }

    @Override
    public void deleteDesignation(
            Long companyId,
            Long designationId
    ) {

        getCompanyOrThrow(companyId);

        Designation designation =
                getDesignationForCompanyOrThrow(
                        companyId,
                        designationId
                );

        designation.setIsDeleted(true);
        designation.setDeletedAt(Instant.now());

        designationRepository.save(designation);
    }

    private InsuranceCompany getCompanyOrThrow(Long companyId) {

        return insuranceCompanyRepository
                .findByIdAndIsDeletedFalse(companyId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "COMPANY_NOT_FOUND",
                                "Insurance company not found"
                        )
                );
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

    private Designation getDesignationForCompanyOrThrow(
            Long companyId,
            Long designationId
    ) {

        Designation designation =
                getDesignationOrThrow(designationId);

        if (!designation.getTenantId().equals(companyId)) {
            throw new NotFoundException(
                    "DESIGNATION_NOT_FOUND",
                    "Designation not found"
            );
        }

        return designation;
    }

    private void validateDuplicateDesignationCode(
            Long companyId,
            String code
    ) {

        if (designationRepository.existsByTenantIdAndCodeAndIsDeletedFalse(companyId, code)) {
            throw new BusinessException(
                    "DESIGNATION_CODE_ALREADY_EXISTS",
                    "Designation code already exists"
            );
        }
    }

    private void validateDuplicateDesignationName(
            Long companyId,
            String name
    ) {

        if (designationRepository.existsByTenantIdAndNameAndIsDeletedFalse(companyId, name)) {
            throw new BusinessException(
                    "DESIGNATION_NAME_ALREADY_EXISTS",
                    "Designation name already exists"
            );
        }
    }
}