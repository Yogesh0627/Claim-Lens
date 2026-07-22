package com.niyotechnologies.claimlens.processing.repository;

import com.niyotechnologies.claimlens.processing.entity.OcrResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Plain entity (no @TenantId) — worker-written. */
@Repository
public interface OcrResultRepository extends JpaRepository<OcrResult, Long> {

    List<OcrResult> findAllByClaimIdOrderByCreatedAtAsc(Long claimId);
}
