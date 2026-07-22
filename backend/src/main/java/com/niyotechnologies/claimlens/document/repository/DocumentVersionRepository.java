package com.niyotechnologies.claimlens.document.repository;

import com.niyotechnologies.claimlens.document.entity.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Tenant-scoped by @TenantId. */
@Repository
public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {

    List<DocumentVersion> findAllByDocumentIdAndIsDeletedFalseOrderByVersionNumberDesc(Long documentId);

    Optional<DocumentVersion> findByIdAndDocumentIdAndIsDeletedFalse(Long id, Long documentId);

    Optional<DocumentVersion> findFirstByDocumentIdAndIsDeletedFalseOrderByVersionNumberDesc(Long documentId);
}
