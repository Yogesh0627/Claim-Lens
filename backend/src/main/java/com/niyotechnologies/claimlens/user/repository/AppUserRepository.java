package com.niyotechnologies.claimlens.user.repository;

import com.niyotechnologies.claimlens.user.entity.AppUser;
import com.niyotechnologies.claimlens.user.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    /**
     * Set a password (and activate) from an ANONYMOUS request — redeeming an invitation or reset
     * link. Native for the same reason as the lookups above: there is no tenant in context, so a
     * JPA update on this @TenantId entity would be scoped to the wrong tenant and touch no rows.
     */
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE app_user SET password_hash = :passwordHash, status = :status "
            + "WHERE id = :id AND is_deleted = false", nativeQuery = true)
    int updateCredentialsForAuthentication(@Param("id") Long id,
                                           @Param("passwordHash") String passwordHash,
                                           @Param("status") String status);

    // Tenant-scoped by @TenantId (only sees the current tenant's users).
    Optional<AppUser> findByIdAndIsDeletedFalse(Long id);

    // The self-service login for a policyholder, if one exists (tenant-scoped by @TenantId).
    Optional<AppUser> findFirstByCustomerIdAndIsDeletedFalse(Long customerId);

    // Eligible-investigator lookup (tenant-scoped by @TenantId).
    List<AppUser> findAllByRoleIdAndStatusAndIsDeletedFalse(Long roleId, UserStatus status);

    // Directory listings (tenant-scoped by @TenantId).
    List<AppUser> findAllByIsDeletedFalseOrderByFirstNameAsc();

    List<AppUser> findAllByRoleIdAndIsDeletedFalseOrderByFirstNameAsc(Long roleId);

    /** Paged directory listing for the Users screen (tenant-scoped by @TenantId). */
    Page<AppUser> findAllByIsDeletedFalse(Pageable pageable);
}
