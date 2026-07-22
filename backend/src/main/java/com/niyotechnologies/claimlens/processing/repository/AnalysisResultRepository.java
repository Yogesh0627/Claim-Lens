package com.niyotechnologies.claimlens.processing.repository;

import com.niyotechnologies.claimlens.processing.entity.AnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Plain entity (no @TenantId) — worker-written. */
@Repository
public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {

    List<AnalysisResult> findAllByClaimIdOrderByCreatedAtAsc(Long claimId);

    /**
     * Exact perceptual-hash reuse on a DIFFERENT claim in the same tenant — a strong photo-reuse
     * signal. (Near-duplicate Hamming matching is a later enhancement; exact pHash catches identical
     * re-uploads, the common case.)
     */
    Optional<AnalysisResult> findFirstByTenantIdAndPhashAndClaimIdNot(
            Long tenantId, String phash, Long claimId);
}
