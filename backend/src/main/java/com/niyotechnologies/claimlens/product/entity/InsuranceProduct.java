package com.niyotechnologies.claimlens.product.entity;

import com.niyotechnologies.claimlens.common.entity.TenantAwareEntity;
import com.niyotechnologies.claimlens.product.enums.ProductStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** What the insurer sells (e.g. PRIVATE_CAR_PREMIUM). Tenant-scoped; versioned over time. */
@Entity
@Table(name = "insurance_product")
@Getter
@Setter
public class InsuranceProduct extends TenantAwareEntity {

    @Column(name = "claim_type_id", nullable = false)
    private Long claimTypeId;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status;
}
