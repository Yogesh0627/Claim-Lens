package com.niyotechnologies.claimlens.auth.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        Long userId,
        Long tenantId,
        Long roleId
) {
}
