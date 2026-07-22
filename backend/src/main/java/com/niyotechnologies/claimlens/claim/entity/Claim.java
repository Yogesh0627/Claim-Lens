package com.niyotechnologies.claimlens.claim.entity;

import com.niyotechnologies.claimlens.claim.enums.ClaimStatus;
import com.niyotechnologies.claimlens.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A claim against a policy. Stores the pinned product version (inherited from the policy) plus
 * write-once snapshots (policy_number, vehicle_registration_number) captured at intake.
 */
@Entity
@Table(name = "claim")
@Getter
@Setter
public class Claim extends TenantAwareEntity {

    @UuidGenerator
    @Column(name = "public_id", nullable = false, updatable = false)
    private UUID publicId;

    @Column(name = "claim_number", nullable = false, length = 50)
    private String claimNumber;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "insurance_policy_id", nullable = false)
    private Long insurancePolicyId;

    @Column(name = "insured_vehicle_id")
    private Long insuredVehicleId;

    @Column(name = "insurance_product_id", nullable = false)
    private Long insuranceProductId;

    @Column(name = "insurance_product_version_id", nullable = false)
    private Long insuranceProductVersionId;

    @Column(name = "claim_type_id", nullable = false)
    private Long claimTypeId;

    @Column(name = "policy_number", nullable = false, length = 100)
    private String policyNumber;

    @Column(name = "vehicle_registration_number", length = 32)
    private String vehicleRegistrationNumber;

    @Column(name = "incident_date", nullable = false)
    private LocalDate incidentDate;

    @Column(name = "reported_date")
    private LocalDate reportedDate;

    @Column(name = "claim_amount", precision = 18, scale = 2)
    private BigDecimal claimAmount;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ClaimStatus status;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "reopened_at")
    private Instant reopenedAt;
}
