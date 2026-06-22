package com.niyotechnologies.claimlens.role.controller;

import com.niyotechnologies.claimlens.common.response.ApiResponse;
import com.niyotechnologies.claimlens.role.dto.response.RoleResponse;
import com.niyotechnologies.claimlens.role.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("${claimlens.api.base-path}/roles")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public ApiResponse<List<RoleResponse>> getAllRoles() {

        return ApiResponse.success(
                roleService.getAllRoles()
        );
    }

    @GetMapping("/{roleId}")
    public ApiResponse<RoleResponse> getRole(
            @PathVariable Long roleId
    ) {
        return ApiResponse.success(
                roleService.getRole(roleId)
        );
    }
}