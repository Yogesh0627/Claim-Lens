package com.niyotechnologies.claimlens.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Common id + audit columns. Auditing is driven by Spring Data's AuditingEntityListener:
 * created/updated timestamps and the acting user (from AuditorAware) are populated
 * automatically — no hand-written @PrePersist/@PreUpdate.
 *
 * Soft-delete columns live here for now. When the first append-only (immutable) entity
 * arrives, split these into a SoftDeletableEntity so append-only tables cannot inherit a
 * "make this disappear" switch.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @LastModifiedBy
    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by")
    private Long deletedBy;
}
