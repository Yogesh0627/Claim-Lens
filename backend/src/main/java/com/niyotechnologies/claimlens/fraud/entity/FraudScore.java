package com.niyotechnologies.claimlens.fraud.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** Append-only fraud result for a claim. Not @TenantId — written by the fraud worker. */
@Entity
@Table(name = "fraud_score")
@Getter
@Setter
public class FraudScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "claim_id", nullable = false)
    private Long claimId;

    @Column(nullable = false)
    private Integer score;

    @Column(name = "risk_level", nullable = false, length = 10)
    private String riskLevel;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
