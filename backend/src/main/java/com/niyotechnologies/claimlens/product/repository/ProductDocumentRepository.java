package com.niyotechnologies.claimlens.product.repository;

import com.niyotechnologies.claimlens.product.entity.ProductDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Tenant-scoped by @TenantId. */
@Repository
public interface ProductDocumentRepository extends JpaRepository<ProductDocument, Long> {

    List<ProductDocument>
    findAllByInsuranceProductVersionIdAndIsDeletedFalseOrderByCreatedAtDesc(Long versionId);

    Optional<ProductDocument> findByIdAndIsDeletedFalse(Long id);
}
