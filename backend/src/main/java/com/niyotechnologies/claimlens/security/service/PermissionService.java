package com.niyotechnologies.claimlens.security.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Resolves a role's permission codes server-side (D11: the JWT carries only roleId, never a
 * permissions array). role/permission/role_permission are global (no tenant_id), so this bypasses
 * the tenant filter by design via JdbcTemplate.
 *
 * <p>Resolved on every authenticated request, so it is cached ({@code rolePermissions}, keyed by
 * roleId) — Caffeine in local dev, Redis (Upstash) in prod, via Spring's cache abstraction. No
 * runtime eviction hook exists because role→permission mappings are migration-managed (V6 seed):
 * a change is a redeploy, which clears the cache; the 10-minute TTL is the backstop. If a runtime
 * permission-management endpoint is ever added, it must {@code @CacheEvict(value="rolePermissions")}.
 * Caching is disabled under the test profile ({@code spring.cache.type=none}) so the RBAC gate test
 * exercises the uncached D11 path (a revoked permission denied on the very next request).
 */
@Service
@RequiredArgsConstructor
public class PermissionService {

    @Autowired
    private final JdbcTemplate jdbcTemplate;

    @Cacheable(value = "rolePermissions", key = "#roleId", unless = "#result.isEmpty()")
    public Set<String> permissionCodesForRole(Long roleId) {
        if (roleId == null) {
            return Set.of();
        }
        List<String> codes = jdbcTemplate.queryForList(
                "SELECT p.code FROM permission p "
                        + "JOIN role_permission rp ON rp.permission_id = p.id "
                        + "WHERE rp.role_id = ?",
                String.class, roleId);
        return new HashSet<>(codes);
    }
}
