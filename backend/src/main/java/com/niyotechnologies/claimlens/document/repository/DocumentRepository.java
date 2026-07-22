package com.niyotechnologies.claimlens.document.repository;

import com.niyotechnologies.claimlens.document.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Tenant-scoped by @TenantId. */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findAllByClaimIdAndIsDeletedFalse(Long claimId);

    Optional<Document> findByIdAndClaimIdAndIsDeletedFalse(Long id, Long claimId);

    /**
     * OCR-worker fetch: native so it bypasses @TenantId (the worker runs outside a request; the tenant
     * is already known from the job). Same rationale as the auth repository's native lookups.
     */
    @Query(value = "SELECT id AS id, storage_key AS storageKey, file_name AS fileName, "
            + "content_type AS contentType, document_type AS documentType "
            + "FROM document WHERE claim_id = :claimId AND is_deleted = false",
            nativeQuery = true)
    List<DocumentOcrView> findForOcrByClaimId(@Param("claimId") Long claimId);
}
