package com.niyotechnologies.claimlens.organization.mapper;

import com.niyotechnologies.claimlens.organization.dto.request.CreateDepartmentRequest;
import com.niyotechnologies.claimlens.organization.dto.request.UpdateDepartmentRequest;
import com.niyotechnologies.claimlens.organization.dto.response.DepartmentResponse;
import com.niyotechnologies.claimlens.organization.entity.Department;
import com.niyotechnologies.claimlens.organization.enums.DepartmentStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DepartmentMapper {

    public DepartmentResponse toResponse(Department department) {
        return DepartmentResponse.builder()
                .id(department.getId())
                .tenantId(department.getTenantId())
                .code(department.getCode())
                .name(department.getName())
                .description(department.getDescription())
                .status(department.getStatus())
                .createdAt(department.getCreatedAt())
                .updatedAt(department.getUpdatedAt())
                .build();
    }


    public Department toEntity(
            CreateDepartmentRequest request
    ) {

        Department department = new Department();

        // tenant_id is populated by Hibernate @TenantId on persist — not set here.
        department.setCode(request.getCode().trim());
        department.setName(request.getName().trim());

        department.setDescription(trim(request.getDescription()));

        department.setStatus(DepartmentStatus.ACTIVE);

        return department;
    }


    public void updateEntity(Department department,
                             UpdateDepartmentRequest request) {

        department.setName(request.getName().trim());
        department.setDescription(
                request.getDescription() == null
                        ? null
                        : request.getDescription().trim()
        );
    }


    public List<DepartmentResponse
            > toResponseList(
            List<Department> departments
    ) {

        return departments.stream()
                .map(this::toResponse)
                .toList();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}