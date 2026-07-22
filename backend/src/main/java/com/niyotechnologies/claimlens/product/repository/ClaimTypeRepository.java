package com.niyotechnologies.claimlens.product.repository;

import com.niyotechnologies.claimlens.product.entity.ClaimType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Global reference data (no @TenantId). */
@Repository
public interface ClaimTypeRepository extends JpaRepository<ClaimType, Long> {

    Optional<ClaimType> findByCode(String code);
}
