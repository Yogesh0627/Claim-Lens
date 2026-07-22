package com.niyotechnologies.claimlens.processing.repository;

import com.niyotechnologies.claimlens.processing.entity.OcrJob;
import com.niyotechnologies.claimlens.processing.enums.JobStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OcrJobRepository extends JpaRepository<OcrJob, Long> {

    /**
     * Claim the next PENDING job with FOR UPDATE SKIP LOCKED: PESSIMISTIC_WRITE takes the row lock,
     * lock.timeout = -2 is Hibernate's SKIP_LOCKED, and Pageable(1) makes it LIMIT 1. Concurrent
     * workers each grab a different job and never block on each other.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("SELECT j FROM OcrJob j WHERE j.status = :status ORDER BY j.createdAt")
    List<OcrJob> findClaimable(@Param("status") JobStatus status, Pageable pageable);

    List<OcrJob> findAllByClaimId(Long claimId);
}
