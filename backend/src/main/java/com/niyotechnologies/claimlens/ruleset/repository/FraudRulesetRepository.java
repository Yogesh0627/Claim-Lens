package com.niyotechnologies.claimlens.ruleset.repository;

import com.niyotechnologies.claimlens.ruleset.entity.FraudRuleset;
import com.niyotechnologies.claimlens.ruleset.enums.RulesetStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Tenant-scoped by @TenantId. */
@Repository
public interface FraudRulesetRepository extends JpaRepository<FraudRuleset, Long> {

    Optional<FraudRuleset> findByIdAndIsDeletedFalse(Long id);

    Optional<FraudRuleset> findByClaimTypeIdAndStatusAndIsDeletedFalse(Long claimTypeId, RulesetStatus status);

    List<FraudRuleset> findAllByIsDeletedFalse();
}
