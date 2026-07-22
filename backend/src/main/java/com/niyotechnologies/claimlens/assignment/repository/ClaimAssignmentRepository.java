package com.niyotechnologies.claimlens.assignment.repository;

import com.niyotechnologies.claimlens.assignment.entity.ClaimAssignment;
import com.niyotechnologies.claimlens.assignment.enums.AssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClaimAssignmentRepository extends JpaRepository<ClaimAssignment, Long> {

    List<ClaimAssignment> findAllByClaimId(Long claimId);

    /** Current workload of an investigator (for the least-loaded strategy). */
    long countByInvestigatorUserIdAndStatus(Long investigatorUserId, AssignmentStatus status);
}
