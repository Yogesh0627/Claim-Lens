package com.niyotechnologies.claimlens.processing.repository;

import com.niyotechnologies.claimlens.processing.entity.ClaimProcessingState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClaimProcessingStateRepository extends JpaRepository<ClaimProcessingState, Long> {

    Optional<ClaimProcessingState> findByClaimId(Long claimId);

    /**
     * The exactly-once fraud gate. A conditional atomic UPDATE on the single processing-state row:
     * Postgres row-locks it, so concurrent OCR/analysis completion handlers serialize here — only the
     * one that finds both stages COMPLETE and fraud NOT_STARTED flips it to QUEUED (returns 1). The
     * loser re-evaluates against the committed state and matches 0 rows. Idempotent.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ClaimProcessingState s SET s.fraudStatus = 'QUEUED' "
            + "WHERE s.claimId = :claimId "
            + "AND s.ocrStatus = 'COMPLETE' AND s.analysisStatus = 'COMPLETE' "
            + "AND s.fraudStatus = 'NOT_STARTED' AND s.pendingReprocess = false")
    int tryQueueFraud(@Param("claimId") Long claimId);
}
