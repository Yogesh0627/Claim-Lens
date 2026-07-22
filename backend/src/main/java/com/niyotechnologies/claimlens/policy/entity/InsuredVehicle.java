package com.niyotechnologies.claimlens.policy.entity;

import com.niyotechnologies.claimlens.common.entity.TenantAwareEntity;
import com.niyotechnologies.claimlens.policy.enums.VehicleStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * The vehicle covered by a motor policy. registration_number_normalized (upper, no spaces/hyphens)
 * is the load-bearing column: every intake validation and duplicate-claim rule compares on it.
 */
@Entity
@Table(name = "insured_vehicle")
@Getter
@Setter
public class InsuredVehicle extends TenantAwareEntity {

    @Column(name = "insurance_policy_id", nullable = false)
    private Long insurancePolicyId;

    @Column(name = "registration_number", nullable = false, length = 32)
    private String registrationNumber;

    @Column(name = "registration_number_normalized", nullable = false, length = 32)
    private String registrationNumberNormalized;

    @Column(nullable = false, length = 100)
    private String make;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(length = 100)
    private String variant;

    @Column(name = "manufacture_year")
    private Integer manufactureYear;

    @Column(name = "chassis_number", length = 64)
    private String chassisNumber;

    @Column(name = "engine_number", length = 64)
    private String engineNumber;

    @Column(length = 50)
    private String colour;

    @Column(name = "fuel_type", length = 30)
    private String fuelType;

    @Column(name = "seating_capacity")
    private Integer seatingCapacity;

    @Column(precision = 18, scale = 2)
    private BigDecimal idv;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VehicleStatus status;

    /** Canonical form for matching: uppercase, alphanumeric only. Never trust client input for this. */
    public static String normalizeRegistration(String registrationNumber) {
        return registrationNumber == null ? null
                : registrationNumber.toUpperCase().replaceAll("[^A-Z0-9]", "");
    }
}
