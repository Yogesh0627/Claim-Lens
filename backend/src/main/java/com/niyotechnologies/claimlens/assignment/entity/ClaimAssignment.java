package com.niyotechnologies.claimlens.assignment.entity;

import com.niyotechnologies.claimlens.assignment.enums.AssignmentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;

/**
 * Links a claim to the investigator handling it. Standalone entity with @TenantId directly (not via
 * TenantAwareEntity/BaseEntity) — workflow infrastructure, so no audit/soft-delete columns.
 */
@Entity
@Table(name = "claim_assignment")
@Getter
@Setter
public class ClaimAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    @Column(name = "claim_id", nullable = false)
    private Long claimId;

    @Column(name = "investigator_user_id", nullable = false)
    private Long investigatorUserId;

    @Column(name = "assigned_by")
    private Long assignedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssignmentStatus status;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;

    @PrePersist
    void onCreate() {
        if (assignedAt == null) {
            assignedAt = Instant.now();
        }
    }
}
