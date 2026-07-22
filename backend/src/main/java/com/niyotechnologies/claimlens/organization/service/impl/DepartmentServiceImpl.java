package com.niyotechnologies.claimlens.organization.service.impl;

import com.niyotechnologies.claimlens.common.exception.BusinessException;
import com.niyotechnologies.claimlens.common.exception.NotFoundException;
import com.niyotechnologies.claimlens.organization.dto.request.CreateDepartmentRequest;
import com.niyotechnologies.claimlens.organization.dto.request.UpdateDepartmentRequest;
import com.niyotechnologies.claimlens.organization.dto.response.DepartmentResponse;
import com.niyotechnologies.claimlens.organization.entity.Department;
import com.niyotechnologies.claimlens.organization.mapper.DepartmentMapper;
import com.niyotechnologies.claimlens.organization.repository.DepartmentRepository;
import com.niyotechnologies.claimlens.organization.service.DepartmentService;
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
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;

    @Override
    @PreAuthorize("hasAuthority('ORG_DEPARTMENT_WRITE')")
    public DepartmentResponse createDepartment(
            CreateDepartmentRequest request
    ) {

        validateDuplicateDepartmentCode(request.getCode());

        validateDuplicateDepartmentName(request.getName());

        // tenant_id is set by Hibernate @TenantId on persist — do not set it here.
        Department department = departmentMapper.toEntity(request);

        Department savedDepartment =
                departmentRepository.save(department);

        return departmentMapper.toResponse(savedDepartment);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ORG_DEPARTMENT_READ')")
    public DepartmentResponse getDepartmentById(
            Long departmentId
    ) {

        Department department = getDepartmentOrThrow(departmentId);

        return departmentMapper.toResponse(department);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ORG_DEPARTMENT_READ')")
    public List<DepartmentResponse> getAllDepartments() {

        List<Department> departments =
                departmentRepository.findAllByIsDeletedFalse();

        return departmentMapper.toResponseList(departments);
    }

    @Override
    @PreAuthorize("hasAuthority('ORG_DEPARTMENT_WRITE')")
    public DepartmentResponse updateDepartment(
            Long departmentId,
            UpdateDepartmentRequest request
    ) {

        Department department = getDepartmentOrThrow(departmentId);

        departmentMapper.updateEntity(
                department,
                request
        );

        Department updatedDepartment =
                departmentRepository.save(department);

        return departmentMapper.toResponse(updatedDepartment);
    }

    @Override
    @PreAuthorize("hasAuthority('ORG_DEPARTMENT_WRITE')")
    public void deleteDepartment(
            Long departmentId
    ) {

        Department department = getDepartmentOrThrow(departmentId);

        department.setIsDeleted(true);
        department.setDeletedAt(Instant.now());

        departmentRepository.save(department);
    }

    private Department getDepartmentOrThrow(
            Long departmentId
    ) {

        return departmentRepository
                .findByIdAndIsDeletedFalse(departmentId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "DEPARTMENT_NOT_FOUND",
                                "Department not found"
                        )
                );
    }

    private void validateDuplicateDepartmentCode(
            String code
    ) {

        if (departmentRepository.existsByCodeAndIsDeletedFalse(code)) {

            throw new BusinessException(
                    "DEPARTMENT_CODE_ALREADY_EXISTS",
                    "Department code already exists"
            );
        }
    }

    private void validateDuplicateDepartmentName(
            String name
    ) {

        if (departmentRepository.existsByNameAndIsDeletedFalse(name)) {

            throw new BusinessException(
                    "DEPARTMENT_NAME_ALREADY_EXISTS",
                    "Department name already exists"
            );
        }
    }
}
