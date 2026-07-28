package com.niyotechnologies.claimlens.user.entity;

import com.niyotechnologies.claimlens.common.entity.TenantAwareEntity;
import com.niyotechnologies.claimlens.user.enums.AuthProvider;
import com.niyotechnologies.claimlens.user.enums.UserStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * A back-office user of a tenant (admin, manager, investigator, …). Tenant-scoped via @TenantId.
 * Note: the login lookup by email must bypass @TenantId (see AppUserRepository) because at login
 * time the tenant is unknown — it is resolved FROM the user.
 */
@Entity
@Table(name = "app_user")
@Getter
@Setter
public class AppUser extends TenantAwareEntity {

    @Column(name = "employee_code", nullable = false, length = 50)
    private String employeeCode;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(name = "password_hash", length = 500)
    private String passwordHash;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    /** Set only for a customer self-service login; NULL for staff users. Links to the policyholder. */
    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "designation_id")
    private Long designationId;

    @Column(name = "home_branch_id")
    private Long homeBranchId;

    @Column(name = "region_id")
    private Long regionId;

    @Column(name = "reporting_manager_id")
    private Long reportingManagerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false, length = 30)
    private AuthProvider authProvider;

    @Column(name = "auth_provider_user_id", length = 255)
    private String authProviderUserId;

    @Column(name = "invitation_token", length = 255)
    private String invitationToken;

    @Column(name = "invitation_expires_at")
    private Instant invitationExpiresAt;

    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;
}
