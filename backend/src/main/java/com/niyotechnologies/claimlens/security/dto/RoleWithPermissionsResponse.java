package com.niyotechnologies.claimlens.security.dto;

import java.util.List;

/**
 * A role and the permissions it grants — the answer to "where are permissions set up?". Permissions
 * attach to the ROLE (not the user); assigning a role grants exactly these. The mapping is
 * migration-seeded, so this view is read-only.
 */
public record RoleWithPermissionsResponse(
        Long id,
        String code,
        String name,
        String description,
        List<PermissionSummary> permissions,
        // True when THIS tenant has customized the role (an override exists); false = inheriting the
        // global default. Lets the UI show a "customized" badge and a "reset to default" affordance.
        boolean customized
) {
}
