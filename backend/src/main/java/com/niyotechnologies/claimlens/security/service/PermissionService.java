package com.niyotechnologies.claimlens.security.service;

import com.niyotechnologies.claimlens.common.exception.BusinessException;
import com.niyotechnologies.claimlens.common.exception.NotFoundException;
import com.niyotechnologies.claimlens.security.dto.PermissionSummary;
import com.niyotechnologies.claimlens.security.dto.RoleWithPermissionsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolves the permission codes a (tenant, role) grants — server-side (D11: the JWT carries only
 * roleId + tenantId, never a permissions array).
 *
 * <p><b>Two-tier RBAC.</b> {@code role_permission} holds the GLOBAL default for each role. A tenant may
 * customize a role: its chosen set lives in {@code tenant_role_permission} and OVERRIDES the default —
 * for that tenant only, so one company's changes never touch another's. A role with no override rows
 * for a tenant simply inherits the global default.
 *
 * <p>Resolved on every authenticated request, so it is cached ({@code rolePermissions}, keyed by
 * {@code tenantId:roleId}). Editing a tenant's override evicts exactly that key, so the change takes
 * effect on the very next request. Caching is disabled under the test profile so the RBAC gate test
 * exercises the uncached path.
 */
@Service
@RequiredArgsConstructor
public class PermissionService {

    private static final String PLATFORM_ADMIN_CODE = "PLATFORM_ADMIN";

    @Autowired
    private final JdbcTemplate jdbcTemplate;

    /**
     * The effective permission codes for a role within a tenant: the tenant's override if it has one,
     * otherwise the global default. This is the hot path — called on every authenticated request.
     */
    @Cacheable(value = "rolePermissions", key = "#tenantId + ':' + #roleId", unless = "#result.isEmpty()")
    public Set<String> permissionCodesForRole(Long tenantId, Long roleId) {
        if (roleId == null) {
            return Set.of();
        }
        if (tenantId != null) {
            List<String> override = jdbcTemplate.queryForList(
                    "SELECT p.code FROM permission p "
                            + "JOIN tenant_role_permission trp ON trp.permission_id = p.id "
                            + "WHERE trp.tenant_id = ? AND trp.role_id = ?",
                    String.class, tenantId, roleId);
            if (!override.isEmpty()) {
                return new HashSet<>(override);
            }
        }
        List<String> defaults = jdbcTemplate.queryForList(
                "SELECT p.code FROM permission p "
                        + "JOIN role_permission rp ON rp.permission_id = p.id "
                        + "WHERE rp.role_id = ?",
                String.class, roleId);
        return new HashSet<>(defaults);
    }

    /**
     * Every role with the permissions EFFECTIVE for the given tenant (its override, or the global
     * default) plus whether the tenant has customized it. Drives the tenant-admin roles view.
     */
    public List<RoleWithPermissionsResponse> rolesWithPermissions(Long tenantId) {
        Map<Long, List<PermissionSummary>> defaults = permsByRole(
                "SELECT rp.role_id AS role_id, p.code, p.name, p.module "
                        + "FROM role_permission rp JOIN permission p ON p.id = rp.permission_id "
                        + "ORDER BY p.module, p.code");
        Map<Long, List<PermissionSummary>> overrides = permsByRole(
                "SELECT trp.role_id AS role_id, p.code, p.name, p.module "
                        + "FROM tenant_role_permission trp JOIN permission p ON p.id = trp.permission_id "
                        + "WHERE trp.tenant_id = ? ORDER BY p.module, p.code",
                tenantId);

        List<RoleWithPermissionsResponse> result = new ArrayList<>();
        for (Map<String, Object> role : jdbcTemplate.queryForList(
                "SELECT id, code, name, description FROM role WHERE is_deleted = FALSE ORDER BY name")) {
            Long roleId = ((Number) role.get("id")).longValue();
            boolean customized = overrides.containsKey(roleId);
            List<PermissionSummary> effective = customized
                    ? overrides.get(roleId)
                    : defaults.getOrDefault(roleId, List.of());
            result.add(new RoleWithPermissionsResponse(
                    roleId, (String) role.get("code"), (String) role.get("name"),
                    (String) role.get("description"), effective, customized));
        }
        return result;
    }

    /** Every permission in the system (code + name + module), for the editor's checkboxes. */
    public List<PermissionSummary> allPermissions() {
        return jdbcTemplate.query(
                "SELECT code, name, module FROM permission ORDER BY module, code",
                (rs, i) -> new PermissionSummary(rs.getString("code"), rs.getString("name"), rs.getString("module")));
    }

    /**
     * Sets a tenant's custom permission set for a role (overriding the global default for that tenant
     * only). Clearing all permissions removes the override, reverting the role to the platform default.
     * Guard rails: the PLATFORM_ADMIN role can't be customized, and the PLATFORM_ADMIN permission can't
     * be granted. Evicts the (tenant, role) cache so it applies on the next request.
     */
    @CacheEvict(value = "rolePermissions", key = "#tenantId + ':' + #roleId")
    public void setTenantRolePermissions(Long tenantId, Long roleId, List<String> permissionCodes) {
        String roleCode;
        try {
            roleCode = jdbcTemplate.queryForObject(
                    "SELECT code FROM role WHERE id = ? AND is_deleted = FALSE", String.class, roleId);
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("ROLE_NOT_FOUND", "Role not found");
        }
        if (PLATFORM_ADMIN_CODE.equals(roleCode)) {
            throw new BusinessException("ROLE_NOT_EDITABLE", "The platform-admin role cannot be customized");
        }

        List<String> codes = permissionCodes == null ? List.of() : permissionCodes;
        if (codes.contains(PLATFORM_ADMIN_CODE)) {
            throw new BusinessException("PERMISSION_NOT_GRANTABLE",
                    "The PLATFORM_ADMIN permission cannot be granted through the role editor");
        }

        // Resolve codes -> ids first so an unknown code fails the whole change (no partial writes).
        List<Long> permissionIds = codes.isEmpty() ? List.of() : resolvePermissionIds(codes);

        jdbcTemplate.update("DELETE FROM tenant_role_permission WHERE tenant_id = ? AND role_id = ?",
                tenantId, roleId);
        for (Long permissionId : permissionIds) {
            jdbcTemplate.update(
                    "INSERT INTO tenant_role_permission (tenant_id, role_id, permission_id) VALUES (?, ?, ?)",
                    tenantId, roleId, permissionId);
        }
    }

    /** Runs a "role_id, code, name, module" query and groups the permissions by role id. */
    private Map<Long, List<PermissionSummary>> permsByRole(String sql, Object... args) {
        Map<Long, List<PermissionSummary>> byRole = new LinkedHashMap<>();
        for (Map<String, Object> row : jdbcTemplate.queryForList(sql, args)) {
            Long roleId = ((Number) row.get("role_id")).longValue();
            byRole.computeIfAbsent(roleId, k -> new ArrayList<>()).add(new PermissionSummary(
                    (String) row.get("code"), (String) row.get("name"), (String) row.get("module")));
        }
        return byRole;
    }

    private List<Long> resolvePermissionIds(List<String> codes) {
        String placeholders = String.join(",", java.util.Collections.nCopies(codes.size(), "?"));
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, code FROM permission WHERE code IN (" + placeholders + ")", codes.toArray());
        if (rows.size() != codes.size()) {
            throw new BusinessException("UNKNOWN_PERMISSION", "One or more permission codes do not exist");
        }
        return rows.stream().map(r -> ((Number) r.get("id")).longValue()).toList();
    }
}
