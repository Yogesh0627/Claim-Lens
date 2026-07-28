package com.niyotechnologies.claimlens.security.dto;

/** One permission, for the roles-and-permissions view. */
public record PermissionSummary(String code, String name, String module) {
}
