package com.niyotechnologies.claimlens.auth.dto;

import java.util.Set;

/**
 * The authenticated caller's identity plus the permission codes resolved server-side for their
 * role. The JWT deliberately carries only roleId (D11), so the frontend cannot read permissions
 * from the token — this endpoint is the single authoritative source it gates the UI on.
 */
public record MeResponse(
        Long userId,
        Long tenantId,
        Long roleId,
        String roleCode,
        String email,
        String employeeCode,
        Long customerId,
        Set<String> permissions
) {
}
