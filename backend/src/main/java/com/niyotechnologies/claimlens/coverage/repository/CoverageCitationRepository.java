package com.niyotechnologies.claimlens.coverage.repository;

import com.niyotechnologies.claimlens.coverage.entity.CoverageCitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoverageCitationRepository extends JpaRepository<CoverageCitation, Long> {

    /** Drop citations pointing at chunks about to be replaced by a re-ingest (tenant-scoped by @TenantId). */
    void deleteByPolicyChunkIdIn(List<Long> policyChunkIds);
}
