package com.niyotechnologies.claimlens.product.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Global reference data (not tenant-scoped): MOTOR, and later HEALTH/PROPERTY/etc.
 * Standalone entity — a small immutable-ish lookup, no soft delete.
 */
@Entity
@Table(name = "claim_type")
@Getter
@Setter
public class ClaimType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = "ACTIVE";
        }
    }
}
