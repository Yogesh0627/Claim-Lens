package com.niyotechnologies.claimlens.ruleset.repository;

import com.niyotechnologies.claimlens.ruleset.entity.FraudRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Tenant-scoped by @TenantId. */
@Repository
public interface FraudRuleRepository extends JpaRepository<FraudRule, Long> {

    List<FraudRule> findAllByFraudRulesetIdAndIsDeletedFalse(Long fraudRulesetId);
}
