package com.niyotechnologies.claimlens.product.entity;

import com.niyotechnologies.claimlens.common.entity.TenantAwareEntity;
import com.niyotechnologies.claimlens.product.enums.ProductVersionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** A dated version of a product's terms. A policy is pinned to one of these at sale. */
@Entity
@Table(name = "insurance_product_version")
@Getter
@Setter
public class InsuranceProductVersion extends TenantAwareEntity {

    @Column(name = "insurance_product_id", nullable = false)
    private Long insuranceProductId;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductVersionStatus status;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "coverage_summary", columnDefinition = "TEXT")
    private String coverageSummary;
}
