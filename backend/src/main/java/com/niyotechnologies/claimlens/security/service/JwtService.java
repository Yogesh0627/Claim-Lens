package com.niyotechnologies.claimlens.security.service;

import com.niyotechnologies.claimlens.security.config.JwtProperties;
import com.niyotechnologies.claimlens.security.model.ClaimLensPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Mints and verifies HS256 access tokens. verifyWith(SecretKey) pins verification to HMAC,
 * so an unsigned ("alg: none") token fails parseSignedClaims — the token's own alg is never trusted.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final java.time.Duration accessTokenTtl;

    public JwtService(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtl = properties.accessTokenTtl();
    }

    public String generateToken(ClaimLensPrincipal principal) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(principal.userId()))
                .claim("tid", principal.tenantId())
                .claim("rid", principal.roleId())
                .claim("email", principal.email())
                .claim("emp", principal.employeeCode())
                .claim("cid", principal.customerId())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtl)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public ClaimLensPrincipal parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return new ClaimLensPrincipal(
                Long.valueOf(claims.getSubject()),
                asLong(claims.get("tid")),
                asLong(claims.get("rid")),
                claims.get("email", String.class),
                claims.get("emp", String.class),
                asLong(claims.get("cid"))
        );
    }

    private static Long asLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }
}
