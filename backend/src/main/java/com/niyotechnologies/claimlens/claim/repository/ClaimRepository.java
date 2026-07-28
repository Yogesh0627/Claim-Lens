package com.niyotechnologies.claimlens.claim.repository;

import com.niyotechnologies.claimlens.claim.entity.Claim;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Tenant-scoped by @TenantId. */
@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {

    Optional<Claim> findByIdAndIsDeletedFalse(Long id);

    boolean existsByClaimNumberAndIsDeletedFalse(String claimNumber);

    List<Claim> findAllByIsDeletedFalse();

    /** Paged staff directory listing (tenant-scoped by @TenantId). */
    Page<Claim> findAllByIsDeletedFalse(Pageable pageable);

    /** Same policy + loss date — used to detect an exact open duplicate at submit. Tenant-scoped. */
    List<Claim> findAllByInsurancePolicyIdAndIncidentDateAndIsDeletedFalse(
            Long insurancePolicyId, LocalDate incidentDate);

    /** Decided claims that carry a ground-truth fraud label — the fraud-evaluation dataset. */
    List<Claim> findAllByFraudConfirmedIsNotNullAndIsDeletedFalse();

    // --- customer portal: ownership-scoped (below tenant) ---
    List<Claim> findAllByCustomerIdAndIsDeletedFalseOrderByIdDesc(Long customerId);

    Optional<Claim> findByIdAndCustomerIdAndIsDeletedFalse(Long id, Long customerId);

    /** [status, count] rows; tenant-scoped automatically by @TenantId. */
    @Query("SELECT c.status, COUNT(c) FROM Claim c WHERE c.isDeleted = false GROUP BY c.status")
    List<Object[]> countGroupedByStatus();
}
