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
@RequestMapping("${claimlens.api.base-path}/organizations")
public class DepartmentController {

    private final DepartmentService departmentService;

    @PostMapping("/departments")
    public ApiResponse<DepartmentResponse> createDepartment(
            @Valid @RequestBody CreateDepartmentRequest request
    ) {

        return ApiResponse.success(
                departmentService.createDepartment(request)
        );
    }

    @GetMapping("/departments/{departmentId}")
    public ApiResponse<DepartmentResponse> getDepartmentById(
            @PathVariable Long departmentId
    ) {

        return ApiResponse.success(
                departmentService.getDepartmentById(departmentId)
        );
    }

    @GetMapping("/departments")
    public ApiResponse<List<DepartmentResponse>> getAllDepartments() {

        return ApiResponse.success(
                departmentService.getAllDepartments()
        );
    }

    @PutMapping("/departments/{departmentId}")
    public ApiResponse<DepartmentResponse> updateDepartment(
            @PathVariable Long departmentId,
            @Valid @RequestBody UpdateDepartmentRequest request
    ) {

        return ApiResponse.success(
                departmentService.updateDepartment(
                        departmentId,
                        request
                )
        );
    }

    @DeleteMapping("/departments/{departmentId}")
    public ApiResponse<Void> deleteDepartment(
            @PathVariable Long departmentId
    ) {

        departmentService.deleteDepartment(departmentId);

        return ApiResponse.success(null);
    }
}
