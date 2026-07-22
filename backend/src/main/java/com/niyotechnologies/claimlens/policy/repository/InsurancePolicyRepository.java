package com.niyotechnologies.claimlens.policy.repository;

import com.niyotechnologies.claimlens.policy.entity.InsurancePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Tenant-scoped by @TenantId. */
@Repository
public interface InsurancePolicyRepository extends JpaRepository<InsurancePolicy, Long> {

    Optional<InsurancePolicy> findByIdAndIsDeletedFalse(Long id);

    Optional<InsurancePolicy> findByPolicyNumberAndIsDeletedFalse(String policyNumber);

    boolean existsByPolicyNumberAndIsDeletedFalse(String policyNumber);

    List<InsurancePolicy> findAllByIsDeletedFalse();

    /** Customer portal: the caller's own policies (ownership-scoped below tenant). */
    List<InsurancePolicy> findAllByCustomerIdAndIsDeletedFalse(Long customerId);
}
