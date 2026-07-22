package com.niyotechnologies.claimlens.user.repository;

import com.niyotechnologies.claimlens.user.entity.AppUser;
import com.niyotechnologies.claimlens.user.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    /**
     * Authentication lookup. Uses a NATIVE query on purpose: native SQL is not rewritten with the
     * @TenantId discriminator, so it finds the user across tenants — which is required because at
     * login the tenant is not yet known (we resolve it from the user, email being globally unique).
     * Regular (HQL/derived) queries stay tenant-scoped.
     */
    @Query(value = "SELECT * FROM app_user WHERE email = :email AND is_deleted = false",
            nativeQuery = true)
    Optional<AppUser> findByEmailForAuthentication(@Param("email") String email);

    /**
     * Cross-tenant lookup by id, used during token refresh (also a pre-authentication operation).
     * Native on purpose: Hibernate fixes the session's tenant when the Session opens, so a
     * mid-transaction TenantContext change cannot re-scope a normal query — the bypass must be
     * baked into the SQL.
     */
    @Query(value = "SELECT * FROM app_user WHERE id = :id AND is_deleted = false",
            nativeQuery = true)
    Optional<AppUser> findByIdForAuthentication(@Param("id") Long id);

    // Tenant-scoped by @TenantId (only sees the current tenant's users).
    Optional<AppUser> findByIdAndIsDeletedFalse(Long id);

    // The self-service login for a policyholder, if one exists (tenant-scoped by @TenantId).
    Optional<AppUser> findFirstByCustomerIdAndIsDeletedFalse(Long customerId);

    // Eligible-investigator lookup (tenant-scoped by @TenantId).
    List<AppUser> findAllByRoleIdAndStatusAndIsDeletedFalse(Long roleId, UserStatus status);

    // Directory listings (tenant-scoped by @TenantId).
    List<AppUser> findAllByIsDeletedFalseOrderByFirstNameAsc();

    List<AppUser> findAllByRoleIdAndIsDeletedFalseOrderByFirstNameAsc(Long roleId);
}
