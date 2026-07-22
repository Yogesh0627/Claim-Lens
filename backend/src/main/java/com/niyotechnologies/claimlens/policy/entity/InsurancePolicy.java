package com.niyotechnologies.claimlens.policy.entity;

import com.niyotechnologies.claimlens.common.entity.TenantAwareEntity;
import com.niyotechnologies.claimlens.policy.enums.PolicyStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * The insurance contract. Pinned to a product VERSION at sale (insuranceProductVersionId): a claim
 * later inherits that version, so it is judged against the terms the customer actually agreed to.
 */
@Entity
@Table(name = "insurance_policy")
@Getter
@Setter
public class InsurancePolicy extends TenantAwareEntity {

    @UuidGenerator
    @Column(name = "public_id", nullable = false, updatable = false)
    private UUID publicId;

    @Column(name = "policy_number", nullable = false, length = 100)
    private String policyNumber;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "insurance_product_id", nullable = false)
    private Long insuranceProductId;

    @Column(name = "insurance_product_version_id", nullable = false)
    private Long insuranceProductVersionId;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to", nullable = false)
    private LocalDate effectiveTo;

    @Column(name = "sum_insured", nullable = false, precision = 18, scale = 2)
    private BigDecimal sumInsured;

    @Column(precision = 18, scale = 2)
    private BigDecimal deductible;

    @Column(name = "premium_amount", precision = 18, scale = 2)
    private BigDecimal premiumAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PolicyStatus status;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "issuing_branch_id")
    private Long issuingBranchId;

    /** Whether this policy provides cover on the given date (used at claim intake). */
    public boolean isActiveOn(LocalDate date) {
        if (status == PolicyStatus.CANCELLED || status == PolicyStatus.DRAFT) {
            return false;
        }
        boolean withinTerm = !date.isBefore(effectiveFrom) && !date.isAfter(effectiveTo);
        boolean notCancelledBefore = cancelledAt == null || date.isBefore(
                cancelledAt.atZone(java.time.ZoneOffset.UTC).toLocalDate());
        return withinTerm && notCancelledBefore;
    }
}
