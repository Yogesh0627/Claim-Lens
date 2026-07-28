package com.niyotechnologies.claimlens.coverage.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;

/** Links a coverage answer to the policy chunks it was grounded on (with retrieval rank + score). */
@Entity
@Table(name = "coverage_answer_citation")
@Getter
@Setter
public class CoverageCitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    @Column(name = "coverage_answer_id", nullable = false)
    private Long coverageAnswerId;

    @Column(name = "policy_chunk_id", nullable = false)
    private Long policyChunkId;

    @Column(name = "citation_rank", nullable = false)
    private Integer citationRank;

    @Column(precision = 7, scale = 6)
    private BigDecimal score;
}
