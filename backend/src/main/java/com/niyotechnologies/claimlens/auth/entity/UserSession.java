package com.niyotechnologies.claimlens.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * A refresh-token session. Deliberately NOT tenant-aware (no @TenantId): a refresh request arrives
 * with no JWT, so the lookup by token hash must not be tenant-filtered. tenant_id is carried as a
 * plain column and restored into TenantContext once the session is validated.
 *
 * Standalone entity (not extending BaseEntity/TenantAwareEntity) — it is session infrastructure, not
 * a soft-deletable business aggregate.
 */
@Entity
@Table(name = "user_session")
@Getter
@Setter
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "refresh_token_hash", nullable = false, length = 64)
    private String refreshTokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "replaced_by_session_id")
    private Long replacedBySessionId;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public boolean isActive(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }
}
