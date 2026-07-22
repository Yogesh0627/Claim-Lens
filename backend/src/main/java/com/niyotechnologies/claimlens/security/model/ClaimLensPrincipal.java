package com.niyotechnologies.claimlens.security.model;

/**
 * The authenticated principal, built from the JWT claims. Stateless — no DB lookup on each
 * request. Permissions are resolved separately (server-side) when method security is enabled.
 */
public record ClaimLensPrincipal(
        Long userId,
        Long tenantId,
        Long roleId,
        String email,
        String employeeCode,
        Long customerId
) {
}
