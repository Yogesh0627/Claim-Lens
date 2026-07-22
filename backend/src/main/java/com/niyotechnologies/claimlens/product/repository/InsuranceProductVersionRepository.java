package com.niyotechnologies.claimlens.product.repository;

import com.niyotechnologies.claimlens.product.entity.InsuranceProductVersion;
import com.niyotechnologies.claimlens.product.enums.ProductVersionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Tenant-scoped by @TenantId. */
@Repository
public interface InsuranceProductVersionRepository
        extends JpaRepository<InsuranceProductVersion, Long> {

    Optional<InsuranceProductVersion> findByIdAndIsDeletedFalse(Long id);

    List<InsuranceProductVersion> findAllByInsuranceProductIdAndIsDeletedFalse(Long productId);

    Optional<InsuranceProductVersion>
    findByInsuranceProductIdAndStatusAndIsDeletedFalse(Long productId, ProductVersionStatus status);

    Optional<InsuranceProductVersion>
    findTopByInsuranceProductIdAndIsDeletedFalseOrderByVersionNumberDesc(Long productId);
}
