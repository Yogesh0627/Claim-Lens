package com.niyotechnologies.claimlens.fraud.repository;

import com.niyotechnologies.claimlens.fraud.entity.FraudScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FraudScoreRepository extends JpaRepository<FraudScore, Long> {

    Optional<FraudScore> findFirstByClaimIdOrderByCreatedAtDesc(Long claimId);

    /** [riskLevel, count] rows. FraudScore is not @TenantId, so filter the tenant explicitly. */
    @Query("SELECT f.riskLevel, COUNT(f) FROM FraudScore f WHERE f.tenantId = :tenantId GROUP BY f.riskLevel")
    List<Object[]> countGroupedByRiskLevel(@Param("tenantId") Long tenantId);
}
