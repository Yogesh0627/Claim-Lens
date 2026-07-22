package com.niyotechnologies.claimlens.processing.repository;

import com.niyotechnologies.claimlens.processing.entity.AnalysisJob;
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
public interface AnalysisJobRepository extends JpaRepository<AnalysisJob, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("SELECT j FROM AnalysisJob j WHERE j.status = :status ORDER BY j.createdAt")
    List<AnalysisJob> findClaimable(@Param("status") JobStatus status, Pageable pageable);

    List<AnalysisJob> findAllByClaimId(Long claimId);
}
