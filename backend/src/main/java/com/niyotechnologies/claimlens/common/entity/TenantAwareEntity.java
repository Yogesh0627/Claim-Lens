package com.niyotechnologies.claimlens.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

/**
 * Base for every tenant-scoped entity. The @TenantId discriminator is enforced inside
 * Hibernate's entity loader — it is appended to SELECT/UPDATE/DELETE including load-by-id,
 * and set automatically on INSERT from the current tenant. It cannot be forgotten.
 *
 * Tenancy is therefore a type-level property: an entity either extends this class and is
 * protected, or extends BaseEntity and is visibly, deliberately global (e.g. InsuranceCompany
 * the tenant root, Role, Permission).
 */
@Getter
@Setter
@MappedSuperclass
public abstract class TenantAwareEntity extends BaseEntity {

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;
}
