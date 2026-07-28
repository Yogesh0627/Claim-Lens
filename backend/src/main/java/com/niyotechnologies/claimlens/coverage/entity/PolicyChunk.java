package com.niyotechnologies.claimlens.coverage.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;

/**
 * One embedded chunk of a product version's policy wording. @TenantId makes every read/write auto
 * tenant-scoped (D7: no cross-tenant leak via a forgotten join). Embedding is stored as a JSON float
 * array (no pgvector at V1); cosine ranking happens in the service.
 */
@Entity
@Table(name = "policy_chunk")
@Getter
@Setter
public class PolicyChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    @Column(name = "insurance_product_id", nullable = false)
    private Long insuranceProductId;

    @Column(name = "insurance_product_version_id", nullable = false)
    private Long insuranceProductVersionId;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(name = "chunk_text", nullable = false, columnDefinition = "text")
    private String chunkText;

    @Column(nullable = false, columnDefinition = "text")
    private String embedding;

    @Column(name = "embedding_model", nullable = false, length = 100)
    private String embeddingModel;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
