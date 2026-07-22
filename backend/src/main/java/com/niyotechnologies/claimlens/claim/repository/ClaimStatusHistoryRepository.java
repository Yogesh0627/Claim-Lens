package com.niyotechnologies.claimlens.claim.repository;

import com.niyotechnologies.claimlens.claim.entity.ClaimStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClaimStatusHistoryRepository extends JpaRepository<ClaimStatusHistory, Long> {

    List<ClaimStatusHistory> findAllByClaimIdOrderByChangedAtAsc(Long claimId);
}
