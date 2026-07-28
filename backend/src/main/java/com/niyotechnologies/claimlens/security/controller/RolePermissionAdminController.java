package com.niyotechnologies.claimlens.security.controller;

import com.niyotechnologies.claimlens.common.response.ApiResponse;
import com.niyotechnologies.claimlens.security.dto.PermissionSummary;
import com.niyotechnologies.claimlens.security.dto.UpdateRolePermissionsRequest;
import com.niyotechnologies.claimlens.security.service.PermissionService;
import com.niyotechnologies.claimlens.tenancy.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Editing what a role grants FOR THE CALLER'S TENANT. Writes a per-tenant override (never the global
 * default), so a tenant admin customizes their own company's roles without affecting anyone else —
 * hence USER_WRITE (a normal tenant-admin capability), not a platform operation.
 */
@RestController
@RequestMapping("${claimlens.api.base-path}/roles")
@RequiredArgsConstructor
public class RolePermissionAdminController {

    @Autowired
    private final PermissionService permissionService;

    /** Every permission in the system, for the editor to render as checkboxes. */
    @GetMapping("/permission-catalog")
    @PreAuthorize("hasAuthority('USER_READ')")
    public ApiResponse<List<PermissionSummary>> permissionCatalog() {
        return ApiResponse.success(permissionService.allPermissions());
    }

    /** Set this TENANT's permissions for a role (a per-tenant override). Applies on the next request. */
    @PutMapping("/{roleId}/permissions")
    @PreAuthorize("hasAuthority('USER_WRITE')")
    public ApiResponse<Void> updatePermissions(
            @PathVariable Long roleId,
            @RequestBody UpdateRolePermissionsRequest request) {
        permissionService.setTenantRolePermissions(
                TenantContext.getTenantId(), roleId, request.permissionCodes());
        return ApiResponse.success(null);
    }
}
