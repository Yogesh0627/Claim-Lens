package com.niyotechnologies.claimlens.claim.repository;

import com.niyotechnologies.claimlens.claim.entity.Claim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Tenant-scoped by @TenantId. */
@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {

    Optional<Claim> findByIdAndIsDeletedFalse(Long id);

    boolean existsByClaimNumberAndIsDeletedFalse(String claimNumber);

    List<Claim> findAllByIsDeletedFalse();

    // --- customer portal: ownership-scoped (below tenant) ---
    List<Claim> findAllByCustomerIdAndIsDeletedFalseOrderByIdDesc(Long customerId);

    Optional<Claim> findByIdAndCustomerIdAndIsDeletedFalse(Long id, Long customerId);

    /** [status, count] rows; tenant-scoped automatically by @TenantId. */
    @Query("SELECT c.status, COUNT(c) FROM Claim c WHERE c.isDeleted = false GROUP BY c.status")
    List<Object[]> countGroupedByStatus();
}
