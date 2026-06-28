package com.niyotechnologies.claimlens.organization.controller;

import com.niyotechnologies.claimlens.common.response.ApiResponse;
import com.niyotechnologies.claimlens.organization.dto.request.CreateDepartmentRequest;
import com.niyotechnologies.claimlens.organization.dto.request.UpdateDepartmentRequest;
import com.niyotechnologies.claimlens.organization.dto.response.DepartmentResponse;
import com.niyotechnologies.claimlens.organization.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("${claimlens.api.base-path}/organizations/companies/{companyId}/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    @PostMapping
    public ApiResponse<DepartmentResponse> createDepartment(
            @PathVariable Long companyId,
            @Valid @RequestBody CreateDepartmentRequest request
    ) {

        return ApiResponse.success(
                departmentService.createDepartment(
                        companyId,
                        request
                )
        );
    }

    @GetMapping("/{departmentId}")
    public ApiResponse<DepartmentResponse> getDepartmentById(
            @PathVariable Long companyId,
            @PathVariable Long departmentId
    ) {

        return ApiResponse.success(
                departmentService.getDepartmentById(
                        companyId,
                        departmentId
                )
        );
    }

    @GetMapping
    public ApiResponse<List<DepartmentResponse>> getAllDepartments(
            @PathVariable Long companyId
    ) {

        return ApiResponse.success(
                departmentService.getAllDepartments(companyId)
        );
    }

    @PutMapping("/{departmentId}")
    public ApiResponse<DepartmentResponse> updateDepartment(
            @PathVariable Long companyId,
            @PathVariable Long departmentId,
            @Valid @RequestBody UpdateDepartmentRequest request
    ) {

        return ApiResponse.success(
                departmentService.updateDepartment(
                        companyId,
                        departmentId,
                        request
                )
        );
    }

    @DeleteMapping("/{departmentId}")
    public ApiResponse<Void> deleteDepartment(
            @PathVariable Long companyId,
            @PathVariable Long departmentId
    ) {

        departmentService.deleteDepartment(
                companyId,
                departmentId
        );

        return ApiResponse.success(null);
    }
}