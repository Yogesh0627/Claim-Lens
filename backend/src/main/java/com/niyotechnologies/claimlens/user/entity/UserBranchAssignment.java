package com.niyotechnologies.claimlens.user.entity;

import com.niyotechnologies.claimlens.common.entity.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Links a staff user to a branch they work at. A user can have several — e.g. a regional manager
 * covering multiple branches — with one flagged {@code isPrimary} (mirrors {@code app_user.home_branch_id}).
 * Tenant-scoped via {@link TenantAwareEntity}.
 */
@Entity
@Table(name = "user_branch_assignment")
@Getter
@Setter
public class UserBranchAssignment extends TenantAwareEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "is_primary", nullable = false)
    private boolean primary = false;
}
