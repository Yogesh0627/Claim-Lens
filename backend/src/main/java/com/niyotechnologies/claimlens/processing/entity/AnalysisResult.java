package com.niyotechnologies.claimlens.processing.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Image analysis signals for one document. Worker-written (plain tenant_id, not @TenantId).
 */
@Entity
@Table(name = "analysis_result")
@Getter
@Setter
public class AnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "claim_id", nullable = false)
    private Long claimId;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(length = 64)
    private String phash;

    @Column(length = 64)
    private String dhash;

    @Column(name = "average_hash", length = 64)
    private String averageHash;

    @Column(name = "exif_state", length = 20)
    private String exifState;

    @Column(name = "synthetic_signal", nullable = false)
    private Boolean syntheticSignal = false;

    @Column(name = "synthetic_score", precision = 5, scale = 4)
    private BigDecimal syntheticScore;

    @Column(name = "duplicate_of_claim_id")
    private Long duplicateOfClaimId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
