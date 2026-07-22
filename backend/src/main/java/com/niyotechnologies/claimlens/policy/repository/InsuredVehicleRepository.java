package com.niyotechnologies.claimlens.policy.repository;

import com.niyotechnologies.claimlens.policy.entity.InsuredVehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Tenant-scoped by @TenantId. */
@Repository
public interface InsuredVehicleRepository extends JpaRepository<InsuredVehicle, Long> {

    List<InsuredVehicle> findAllByInsurancePolicyIdAndIsDeletedFalse(Long policyId);

    Optional<InsuredVehicle> findByRegistrationNumberNormalizedAndIsDeletedFalse(String normalizedReg);
}
