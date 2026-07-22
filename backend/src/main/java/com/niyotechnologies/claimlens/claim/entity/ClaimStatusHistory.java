package com.niyotechnologies.claimlens.claim.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** Append-only record of a claim status transition. Not tenant-filtered by @TenantId — plain column. */
@Entity
@Table(name = "claim_status_history")
@Getter
@Setter
public class ClaimStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "claim_id", nullable = false)
    private Long claimId;

    @Column(name = "from_status", length = 30)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 30)
    private String toStatus;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "changed_by")
    private Long changedBy;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    @PrePersist
    void onCreate() {
        if (changedAt == null) {
            changedAt = Instant.now();
        }
    }
}
