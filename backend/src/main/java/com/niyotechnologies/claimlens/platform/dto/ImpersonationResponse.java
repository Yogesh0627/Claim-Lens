package com.niyotechnologies.claimlens.platform.dto;

/**
 * A short-lived, tenant-scoped access token that lets the platform admin operate inside a tenant's
 * workspace (as its admin) through the normal app. Access-only (no refresh) — impersonation expires
 * and the platform admin returns to the console.
 */
public record ImpersonationResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        Long tenantId,
        String tenantName
) {
}
