package com.niyotechnologies.claimlens.assignment.repository;

import com.niyotechnologies.claimlens.assignment.entity.ClaimAssignment;
import com.niyotechnologies.claimlens.assignment.enums.AssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ClaimAssignmentRepository extends JpaRepository<ClaimAssignment, Long> {

    List<ClaimAssignment> findAllByClaimId(Long claimId);

    /** Batch variant — assignments for a whole page of claims in one query (avoids N+1 in the mapper). */
    List<ClaimAssignment> findAllByClaimIdIn(Collection<Long> claimIds);

    /** Current workload of an investigator (for the least-loaded strategy). */
    long countByInvestigatorUserIdAndStatus(Long investigatorUserId, AssignmentStatus status);
}
