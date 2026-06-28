package com.niyotechnologies.claimlens.organization.service.impl;

import com.niyotechnologies.claimlens.common.exception.BusinessException;
import com.niyotechnologies.claimlens.common.exception.NotFoundException;
import com.niyotechnologies.claimlens.organization.dto.request.CreateDepartmentRequest;
import com.niyotechnologies.claimlens.organization.dto.request.UpdateDepartmentRequest;
import com.niyotechnologies.claimlens.organization.dto.response.DepartmentResponse;
import com.niyotechnologies.claimlens.organization.entity.Department;
import com.niyotechnologies.claimlens.organization.entity.InsuranceCompany;
import com.niyotechnologies.claimlens.organization.mapper.DepartmentMapper;
import com.niyotechnologies.claimlens.organization.repository.DepartmentRepository;
import com.niyotechnologies.claimlens.organization.repository.InsuranceCompanyRepository;
import com.niyotechnologies.claimlens.organization.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final InsuranceCompanyRepository insuranceCompanyRepository;
    private final DepartmentMapper departmentMapper;


    private Department getDepartmentForCompanyOrThrow(
            Long companyId,
            Long departmentId
    ) {

        Department department = getDepartmentOrThrow(departmentId);

        if (!department.getTenantId().equals(companyId)) {
            throw new NotFoundException(
                    "DEPARTMENT_NOT_FOUND",
                    "Department not found"
            );
        }

        return department;
    }

    @Override
    public DepartmentResponse createDepartment(
            Long companyId,
            CreateDepartmentRequest request
    ) {

        getCompanyOrThrow(companyId);

        validateDuplicateDepartmentCode(
                companyId,
                request.getCode()
        );

        validateDuplicateDepartmentName(
                companyId,
                request.getName()
        );

        Department department =
                departmentMapper.toEntity(
                        companyId,
                        request
                );

        Department savedDepartment =
                departmentRepository.save(department);

        return departmentMapper.toResponse(savedDepartment);
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentById(
            Long companyId,
            Long departmentId
    ) {

        getCompanyOrThrow(companyId);

        Department department =
                getDepartmentForCompanyOrThrow(
                        companyId,
                        departmentId
                );

        return departmentMapper.toResponse(department);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAllDepartments(
            Long companyId
    ) {

        getCompanyOrThrow(companyId);

        List<Department> departments =
                departmentRepository.findAllByTenantIdAndIsDeletedFalse(companyId);

        return departmentMapper.toResponseList(departments);
    }

    @Override
    public DepartmentResponse updateDepartment(
            Long companyId,
            Long departmentId,
            UpdateDepartmentRequest request
    ) {

        getCompanyOrThrow(companyId);

        Department department =
                getDepartmentForCompanyOrThrow(
                        companyId,
                        departmentId
                );

        departmentMapper.updateEntity(
                department,
                request
        );

        Department updatedDepartment =
                departmentRepository.save(department);

        return departmentMapper.toResponse(updatedDepartment);
    }

    @Override
    public void deleteDepartment(
            Long companyId,
            Long departmentId
    ) {

        getCompanyOrThrow(companyId);

        Department department =
                getDepartmentForCompanyOrThrow(
                        companyId,
                        departmentId
                );

        department.setIsDeleted(true);
        department.setDeletedAt(Instant.now());

        departmentRepository.save(department);
    }

    private InsuranceCompany getCompanyOrThrow(
            Long companyId
    ) {

        return insuranceCompanyRepository
                .findByIdAndIsDeletedFalse(companyId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "COMPANY_NOT_FOUND",
                                "Insurance company not found"
                        )
                );
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
            Long tenantId,
            String code
    ) {

        if (departmentRepository.existsByTenantIdAndCodeAndIsDeletedFalse(
                tenantId,
                code
        )) {

            throw new BusinessException(
                    "DEPARTMENT_CODE_ALREADY_EXISTS",
                    "Department code already exists"
            );
        }
    }

    private void validateDuplicateDepartmentName(
            Long tenantId,
            String name
    ) {

        if (departmentRepository.existsByTenantIdAndNameAndIsDeletedFalse(
                tenantId,
                name
        )) {

            throw new BusinessException(
                    "DEPARTMENT_NAME_ALREADY_EXISTS",
                    "Department name already exists"
            );
        }
    }
}