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

    /** Batch lookup for a page of ids (avoids N+1 in the claim mapper). */
    List<InsuranceProductVersion> findAllByIdInAndIsDeletedFalse(java.util.Collection<Long> ids);

    Optional<InsuranceProductVersion>
    findByInsuranceProductIdAndStatusAndIsDeletedFalse(Long productId, ProductVersionStatus status);

    Optional<InsuranceProductVersion>
    findTopByInsuranceProductIdAndIsDeletedFalseOrderByVersionNumberDesc(Long productId);
}
