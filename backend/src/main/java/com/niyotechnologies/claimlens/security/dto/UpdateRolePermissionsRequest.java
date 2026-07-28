package com.niyotechnologies.claimlens.security.dto;

import java.util.List;

/** The exact set of permission codes a role should grant (replaces the current set). */
public record UpdateRolePermissionsRequest(List<String> permissionCodes) {
}
