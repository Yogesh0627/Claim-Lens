package com.niyotechnologies.claimlens.investigation.repository;

import com.niyotechnologies.claimlens.investigation.entity.InvestigationNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Tenant-scoped by @TenantId. */
@Repository
public interface InvestigationNoteRepository extends JpaRepository<InvestigationNote, Long> {

    List<InvestigationNote> findAllByClaimIdAndIsDeletedFalseOrderByCreatedAtAsc(Long claimId);
}
