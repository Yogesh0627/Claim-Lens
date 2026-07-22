package com.niyotechnologies.claimlens.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * JWT settings, bound from {@code claimlens.security.jwt.*}.
 * The secret has no default (see application.properties) so the app fails fast if unset.
 */
@ConfigurationProperties(prefix = "claimlens.security.jwt")
public record JwtProperties(
        String secret,
        Duration accessTokenTtl,
        Duration refreshTokenTtl
) {
}
