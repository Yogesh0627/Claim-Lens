package com.niyotechnologies.claimlens.security.controller;

import com.niyotechnologies.claimlens.common.response.ApiResponse;
import com.niyotechnologies.claimlens.security.dto.RoleWithPermissionsResponse;
import com.niyotechnologies.claimlens.security.service.PermissionService;
import com.niyotechnologies.claimlens.tenancy.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only roles-and-permissions view: every role with the permissions it grants. Answers "where are
 * permissions set up?" — on the role, seeded by migration. Guarded by USER_READ (whoever manages users
 * can see what each role can do). A literal "/catalog" so it doesn't collide with /roles/{roleId}.
 */
@RestController
@RequestMapping("${claimlens.api.base-path}/roles/catalog")
@RequiredArgsConstructor
public class RoleCatalogController {

    @Autowired
    private final PermissionService permissionService;

    // Tenant-scoped: returns the permissions EFFECTIVE for the caller's tenant (its override, or the
    // global default). USER_READ so any user-manager can see it.
    @GetMapping
    @PreAuthorize("hasAuthority('USER_READ')")
    public ApiResponse<List<RoleWithPermissionsResponse>> catalog() {
        return ApiResponse.success(permissionService.rolesWithPermissions(TenantContext.getTenantId()));
    }
}
