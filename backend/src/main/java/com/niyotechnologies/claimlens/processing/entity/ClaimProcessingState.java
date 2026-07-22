package com.niyotechnologies.claimlens.processing.entity;

import com.niyotechnologies.claimlens.processing.support.ProcessingStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * One row per claim (UNIQUE claim_id). That single row is the lock target for the exactly-once fraud
 * gate. Statuses are plain strings so the gate can be a conditional JPQL UPDATE. Not @TenantId — the
 * orchestrator runs it from workers.
 */
@Entity
@Table(name = "claim_processing_state")
@Getter
@Setter
public class ClaimProcessingState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "claim_id", nullable = false)
    private Long claimId;

    @Column(name = "ocr_status", nullable = false, length = 20)
    private String ocrStatus = ProcessingStatus.NOT_STARTED;

    @Column(name = "analysis_status", nullable = false, length = 20)
    private String analysisStatus = ProcessingStatus.NOT_STARTED;

    @Column(name = "fraud_status", nullable = false, length = 20)
    private String fraudStatus = ProcessingStatus.NOT_STARTED;

    @Column(name = "pending_reprocess", nullable = false)
    private Boolean pendingReprocess = false;

    @Column(name = "fraud_job_id")
    private Long fraudJobId;

    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        lastUpdatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        lastUpdatedAt = Instant.now();
    }
}
