package com.niyotechnologies.claimlens.auth.service.impl;

import com.niyotechnologies.claimlens.auth.dto.GoogleLoginRequest;
import com.niyotechnologies.claimlens.auth.dto.LoginRequest;
import com.niyotechnologies.claimlens.auth.dto.LoginResponse;
import com.niyotechnologies.claimlens.auth.dto.MeResponse;
import com.niyotechnologies.claimlens.auth.dto.RefreshTokenRequest;
import com.niyotechnologies.claimlens.auth.entity.UserSession;
import com.niyotechnologies.claimlens.auth.repository.UserSessionRepository;
import com.niyotechnologies.claimlens.auth.service.AuthService;
import com.niyotechnologies.claimlens.auth.service.GoogleTokenVerifier;
import com.niyotechnologies.claimlens.common.exception.UnauthorizedException;
import com.niyotechnologies.claimlens.role.repository.RoleRepository;
import com.niyotechnologies.claimlens.security.config.JwtProperties;
import com.niyotechnologies.claimlens.security.model.ClaimLensPrincipal;
import com.niyotechnologies.claimlens.security.service.JwtService;
import com.niyotechnologies.claimlens.security.service.PermissionService;
import com.niyotechnologies.claimlens.user.entity.AppUser;
import com.niyotechnologies.claimlens.user.enums.UserStatus;
import com.niyotechnologies.claimlens.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    private static final String INVALID_REFRESH_TOKEN = "INVALID_REFRESH_TOKEN";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AppUserRepository appUserRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final PermissionService permissionService;
    private final RoleRepository roleRepository;
    private final GoogleTokenVerifier googleTokenVerifier;

    @Override
    public LoginResponse login(LoginRequest request) {
        // Native lookup: crosses tenants because the tenant is unknown until we find the user.
        AppUser user = appUserRepository.findByEmailForAuthentication(request.email())
                .orElseThrow(this::invalidCredentials);

        // Same error for unknown user and wrong password — never reveal which.
        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("ACCOUNT_NOT_ACTIVE", "Account is not active");
        }

        return issueTokens(user).response();
    }

    @Override
    public LoginResponse loginWithGoogle(GoogleLoginRequest request) {
        GoogleTokenVerifier.GoogleIdentity identity = googleTokenVerifier.verify(request.credential());

        // Google is an alternate login for EXISTING users only. This is a B2B, multi-tenant system:
        // admins provision users into a tenant — we never auto-create a tenant-less account from an
        // arbitrary Google sign-in. Email is globally unique, so it maps to at most one account.
        AppUser user = appUserRepository.findByEmailForAuthentication(identity.email())
                .orElseThrow(() -> new UnauthorizedException(
                        "NO_ACCOUNT_FOR_EMAIL", "No ClaimLens account exists for this Google email"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("ACCOUNT_NOT_ACTIVE", "Account is not active");
        }
        return issueTokens(user).response();
    }

    @Override
    public LoginResponse refresh(RefreshTokenRequest request) {
        String hash = sha256Hex(request.refreshToken());
        UserSession session = userSessionRepository.findByRefreshTokenHash(hash)
                .orElseThrow(() -> new UnauthorizedException(INVALID_REFRESH_TOKEN, "Invalid refresh token"));

        Instant now = Instant.now();
        if (!session.isActive(now)) {
            throw new UnauthorizedException(INVALID_REFRESH_TOKEN, "Invalid refresh token");
        }

        // Cross-tenant lookup: refresh is pre-authentication. A mid-transaction TenantContext change
        // would NOT re-scope a normal @TenantId query (Hibernate fixes the tenant when the Session
        // opens), so the user must be loaded via the native bypass query.
        AppUser user = appUserRepository.findByIdForAuthentication(session.getUserId())
                .orElseThrow(() -> new UnauthorizedException(INVALID_REFRESH_TOKEN, "Invalid refresh token"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("ACCOUNT_NOT_ACTIVE", "Account is not active");
        }

        // Rotate: issue a new session, then revoke the old one and link them.
        Issued issued = issueTokens(user);
        session.setRevokedAt(now);
        session.setReplacedBySessionId(issued.session().getId());
        userSessionRepository.save(session);

        return issued.response();
    }

    @Override
    @Transactional(readOnly = true)
    public MeResponse me(ClaimLensPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("UNAUTHENTICATED", "No authenticated user");
        }
        // role/permission are global (no @TenantId) — safe to read pre-tenant-scope.
        String roleCode = roleRepository.findByIdAndIsDeletedFalse(principal.roleId())
                .map(role -> role.getCode())
                .orElse(null);
        return new MeResponse(
                principal.userId(),
                principal.tenantId(),
                principal.roleId(),
                roleCode,
                principal.email(),
                principal.employeeCode(),
                principal.customerId(),
                permissionService.permissionCodesForRole(principal.roleId()));
    }

    @Override
    public void logout(RefreshTokenRequest request) {
        userSessionRepository.findByRefreshTokenHash(sha256Hex(request.refreshToken()))
                .ifPresent(session -> {
                    if (session.getRevokedAt() == null) {
                        session.setRevokedAt(Instant.now());
                        userSessionRepository.save(session);
                    }
                });
    }

    private Issued issueTokens(AppUser user) {
        ClaimLensPrincipal principal = new ClaimLensPrincipal(
                user.getId(), user.getTenantId(), user.getRoleId(),
                user.getEmail(), user.getEmployeeCode(), user.getCustomerId());

        String accessToken = jwtService.generateToken(principal);
        String rawRefreshToken = generateRefreshToken();

        UserSession session = new UserSession();
        session.setTenantId(user.getTenantId());
        session.setUserId(user.getId());
        session.setRefreshTokenHash(sha256Hex(rawRefreshToken));
        session.setExpiresAt(Instant.now().plus(jwtProperties.refreshTokenTtl()));
        UserSession saved = userSessionRepository.save(session);

        LoginResponse response = new LoginResponse(
                accessToken,
                rawRefreshToken,
                "Bearer",
                jwtProperties.accessTokenTtl().getSeconds(),
                user.getId(),
                user.getTenantId(),
                user.getRoleId());

        return new Issued(response, saved);
    }

    private UnauthorizedException invalidCredentials() {
        return new UnauthorizedException(INVALID_CREDENTIALS, "Invalid email or password");
    }

    private static String generateRefreshToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private record Issued(LoginResponse response, UserSession session) {
    }
}
