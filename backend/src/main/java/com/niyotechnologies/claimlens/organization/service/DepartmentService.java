package com.niyotechnologies.claimlens.organization.service;

import com.niyotechnologies.claimlens.organization.dto.request.CreateDepartmentRequest;
import com.niyotechnologies.claimlens.organization.dto.request.UpdateDepartmentRequest;
import com.niyotechnologies.claimlens.organization.dto.response.DepartmentResponse;

import java.util.List;

public interface DepartmentService {

    DepartmentResponse createDepartment(
            CreateDepartmentRequest request
    );

    DepartmentResponse updateDepartment(
            Long departmentId,
            UpdateDepartmentRequest request
    );

    DepartmentResponse getDepartmentById(
            Long departmentId
    );

    List<DepartmentResponse> getAllDepartments();

    void deleteDepartment(
            Long departmentId
    );}
