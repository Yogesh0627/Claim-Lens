package com.niyotechnologies.claimlens.processing.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * OCR output for one document. Worker-written (plain tenant_id, not @TenantId) since the OCR worker
 * runs outside any request/tenant context.
 */
@Entity
@Table(name = "ocr_result")
@Getter
@Setter
public class OcrResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "claim_id", nullable = false)
    private Long claimId;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(nullable = false, length = 30)
    private String engine;

    @Column(name = "extracted_text", columnDefinition = "text")
    private String extractedText;

    @Column(precision = 5, scale = 2)
    private BigDecimal confidence;

    @Column(name = "registration_numbers", columnDefinition = "text")
    private String registrationNumbers;

    @Column(name = "policy_numbers", columnDefinition = "text")
    private String policyNumbers;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
