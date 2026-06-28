package com.niyotechnologies.claimlens.organization.service;

import com.niyotechnologies.claimlens.organization.dto.request.CreateDepartmentRequest;
import com.niyotechnologies.claimlens.organization.dto.request.UpdateDepartmentRequest;
import com.niyotechnologies.claimlens.organization.dto.response.DepartmentResponse;

import java.util.List;

public interface DepartmentService {

    DepartmentResponse createDepartment(
            Long companyId,
            CreateDepartmentRequest request
    );

    DepartmentResponse updateDepartment(
            Long companyId,
            Long departmentId,
            UpdateDepartmentRequest request
    );

    DepartmentResponse getDepartmentById(
            Long companyId,
            Long departmentId
    );

    List<DepartmentResponse> getAllDepartments(
            Long companyId
    );

    void deleteDepartment(
            Long companyId,
            Long departmentId
    );}