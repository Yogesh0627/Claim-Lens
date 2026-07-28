package com.niyotechnologies.claimlens.coverage.repository;

import com.niyotechnologies.claimlens.coverage.entity.PolicyChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Tenant-scoped by @TenantId. */
@Repository
public interface PolicyChunkRepository extends JpaRepository<PolicyChunk, Long> {

    List<PolicyChunk> findAllByInsuranceProductVersionId(Long insuranceProductVersionId);

    long countByInsuranceProductVersionId(Long insuranceProductVersionId);

    void deleteByInsuranceProductVersionId(Long insuranceProductVersionId);

    /** Product versions that actually have ingested wording — the ones the AI can answer about. */
    @Query("SELECT DISTINCT c.insuranceProductVersionId FROM PolicyChunk c")
    List<Long> findDistinctVersionIdsWithKnowledge();
}
