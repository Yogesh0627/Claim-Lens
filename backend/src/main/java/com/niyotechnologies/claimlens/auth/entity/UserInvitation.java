package com.niyotechnologies.claimlens.auth.entity;

import com.niyotechnologies.claimlens.auth.enums.InvitationPurpose;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * A single-use, expiring token that lets someone set their own password — either the invitation sent
 * when an admin creates their account, or a self-service password reset.
 *
 * <p>Deliberately NOT tenant-aware (no {@code @TenantId}), for the same reason as {@link UserSession}:
 * the token is redeemed by an anonymous caller with no JWT, so a tenant-filtered lookup would find
 * nothing. tenant_id is carried as a plain column for auditing.
 *
 * <p>Only the SHA-256 hash is persisted — the raw token exists solely in the emailed link, so a
 * database leak yields nothing usable.
 */
@Entity
@Table(name = "user_invitation")
@Getter
@Setter
public class UserInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 20)
    private InvitationPurpose purpose;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** Non-null once redeemed — enforces single use. */
    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public boolean isRedeemable(Instant now) {
        return usedAt == null && now.isBefore(expiresAt);
    }
}
