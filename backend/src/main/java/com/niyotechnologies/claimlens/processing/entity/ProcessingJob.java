package com.niyotechnologies.claimlens.processing.entity;

import com.niyotechnologies.claimlens.processing.enums.JobStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Shared shape for ocr_job / analysis_job / fraud_job. Deliberately NOT tenant-filtered (plain
 * tenant_id, no @TenantId): background workers claim jobs across all tenants.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class ProcessingJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "claim_id", nullable = false)
    private Long claimId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status = JobStatus.PENDING;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;

    @Column(name = "locked_by")
    private String lockedBy;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
