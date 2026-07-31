package com.niyotechnologies.claimlens.product.repository;

import com.niyotechnologies.claimlens.product.entity.InsuranceProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Tenant-scoped by @TenantId. */
@Repository
public interface InsuranceProductRepository extends JpaRepository<InsuranceProduct, Long> {

    Optional<InsuranceProduct> findByIdAndIsDeletedFalse(Long id);

    boolean existsByCodeAndIsDeletedFalse(String code);

    List<InsuranceProduct> findAllByIsDeletedFalse();

    /** Batch lookup for a page of ids (avoids N+1 in the claim mapper). */
    List<InsuranceProduct> findAllByIdInAndIsDeletedFalse(java.util.Collection<Long> ids);
}
