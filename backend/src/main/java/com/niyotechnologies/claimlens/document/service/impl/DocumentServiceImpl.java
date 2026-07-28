package com.niyotechnologies.claimlens.document.service.impl;

import com.niyotechnologies.claimlens.claim.repository.ClaimRepository;
import com.niyotechnologies.claimlens.common.exception.BusinessException;
import com.niyotechnologies.claimlens.common.exception.NotFoundException;
import com.niyotechnologies.claimlens.document.dto.response.DocumentContent;
import com.niyotechnologies.claimlens.document.dto.response.DocumentResponse;
import com.niyotechnologies.claimlens.document.dto.response.DocumentVersionResponse;
import com.niyotechnologies.claimlens.document.entity.Document;
import com.niyotechnologies.claimlens.document.entity.DocumentVersion;
import com.niyotechnologies.claimlens.document.repository.DocumentRepository;
import com.niyotechnologies.claimlens.document.repository.DocumentVersionRepository;
import com.niyotechnologies.claimlens.document.service.DocumentService;
import com.niyotechnologies.claimlens.document.storage.DocumentStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    @Autowired
    private final DocumentRepository documentRepository;
    @Autowired
    private final DocumentVersionRepository documentVersionRepository;
    @Autowired
    private final ClaimRepository claimRepository;
    @Autowired
    private final DocumentStorage documentStorage;

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('CLAIM_WRITE')")
    public DocumentResponse upload(Long claimId, String documentType, MultipartFile file) {
        return uploadInternal(claimId, documentType, file);
    }

    @Override
    @Transactional
    public DocumentResponse uploadInternal(Long claimId, String documentType, MultipartFile file) {
        // The claim must exist within this tenant (tenant-scoped lookup).
        claimRepository.findByIdAndIsDeletedFalse(claimId)
                .orElseThrow(() -> new NotFoundException("CLAIM_NOT_FOUND", "Claim not found"));

        String storageKey = storeOrThrow(file);

        Document document = new Document();
        document.setClaimId(claimId);
        document.setDocumentType(documentType);
        document.setFileName(file.getOriginalFilename());
        document.setContentType(file.getContentType());
        document.setSizeBytes(file.getSize());
        document.setStorageKey(storageKey);
        document.setStatus("UPLOADED");
        Document saved = documentRepository.save(document);

        // Every document has at least version 1; the document's own fields mirror the live version.
        DocumentVersion v1 = newVersion(saved, 1, storageKey, file);
        saved.setCurrentVersionId(v1.getId());
        return toResponse(documentRepository.save(saved));
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('CLAIM_WRITE')")
    public DocumentResponse uploadNewVersion(Long claimId, Long documentId, MultipartFile file) {
        Document document = documentRepository.findByIdAndClaimIdAndIsDeletedFalse(documentId, claimId)
                .orElseThrow(() -> new NotFoundException("DOCUMENT_NOT_FOUND", "Document not found"));

        int nextNumber = backfillLatest(document) + 1;
        String storageKey = storeOrThrow(file);

        DocumentVersion version = newVersion(document, nextNumber, storageKey, file);

        // Repoint the live version and refresh the document's mirror fields to the new upload.
        document.setCurrentVersionId(version.getId());
        document.setFileName(file.getOriginalFilename());
        document.setContentType(file.getContentType());
        document.setSizeBytes(file.getSize());
        document.setStorageKey(storageKey);
        document.setStatus("UPLOADED");
        return toResponse(documentRepository.save(document));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('CLAIM_READ')")
    public List<DocumentResponse> listForClaim(Long claimId) {
        return listForClaimInternal(claimId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> listForClaimInternal(Long claimId) {
        return documentRepository.findAllByClaimIdAndIsDeletedFalse(claimId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('CLAIM_READ')")
    public List<DocumentVersionResponse> listVersions(Long claimId, Long documentId) {
        return listVersionsInternal(claimId, documentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentVersionResponse> listVersionsInternal(Long claimId, Long documentId) {
        Document document = documentRepository.findByIdAndClaimIdAndIsDeletedFalse(documentId, claimId)
                .orElseThrow(() -> new NotFoundException("DOCUMENT_NOT_FOUND", "Document not found"));
        return documentVersionRepository
                .findAllByDocumentIdAndIsDeletedFalseOrderByVersionNumberDesc(document.getId()).stream()
                .map(v -> toVersionResponse(v, document.getCurrentVersionId()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('CLAIM_READ')")
    public DocumentContent download(Long claimId, Long documentId) {
        return downloadInternal(claimId, documentId);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentContent downloadInternal(Long claimId, Long documentId) {
        // Tenant-scoped: @TenantId on Document means a cross-tenant id simply isn't found (404).
        Document document = documentRepository.findByIdAndClaimIdAndIsDeletedFalse(documentId, claimId)
                .orElseThrow(() -> new NotFoundException("DOCUMENT_NOT_FOUND", "Document not found"));
        byte[] content = documentStorage.retrieve(document.getStorageKey());
        return new DocumentContent(content, document.getFileName(), document.getContentType());
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('CLAIM_READ')")
    public DocumentContent downloadVersion(Long claimId, Long documentId, Long versionId) {
        return downloadVersionInternal(claimId, documentId, versionId);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentContent downloadVersionInternal(Long claimId, Long documentId, Long versionId) {
        // Guard the document is in this claim/tenant first, then fetch the requested version.
        documentRepository.findByIdAndClaimIdAndIsDeletedFalse(documentId, claimId)
                .orElseThrow(() -> new NotFoundException("DOCUMENT_NOT_FOUND", "Document not found"));
        DocumentVersion version = documentVersionRepository
                .findByIdAndDocumentIdAndIsDeletedFalse(versionId, documentId)
                .orElseThrow(() -> new NotFoundException("DOCUMENT_VERSION_NOT_FOUND", "Document version not found"));
        byte[] content = documentStorage.retrieve(version.getStorageKey());
        return new DocumentContent(content, version.getFileName(), version.getContentType());
    }

    /**
     * Legacy documents created before versioning existed have no version rows. Materialize a v1 from
     * the document's current snapshot so version numbering stays contiguous, and return the highest
     * version number now present.
     */
    private int backfillLatest(Document document) {
        return documentVersionRepository
                .findFirstByDocumentIdAndIsDeletedFalseOrderByVersionNumberDesc(document.getId())
                .map(DocumentVersion::getVersionNumber)
                .orElseGet(() -> {
                    DocumentVersion v1 = new DocumentVersion();
                    v1.setDocumentId(document.getId());
                    v1.setVersionNumber(1);
                    v1.setFileName(document.getFileName());
                    v1.setContentType(document.getContentType());
                    v1.setSizeBytes(document.getSizeBytes());
                    v1.setStorageKey(document.getStorageKey());
                    v1.setStatus("UPLOADED");
                    DocumentVersion saved = documentVersionRepository.save(v1);
                    if (document.getCurrentVersionId() == null) {
                        document.setCurrentVersionId(saved.getId());
                    }
                    return 1;
                });
    }

    private DocumentVersion newVersion(Document document, int number, String storageKey, MultipartFile file) {
        DocumentVersion version = new DocumentVersion();
        version.setDocumentId(document.getId());
        version.setVersionNumber(number);
        version.setFileName(file.getOriginalFilename());
        version.setContentType(file.getContentType());
        version.setSizeBytes(file.getSize());
        version.setStorageKey(storageKey);
        version.setStatus("UPLOADED");
        return documentVersionRepository.save(version);
    }

    private String storeOrThrow(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("EMPTY_FILE", "Uploaded file is empty");
        }
        try {
            return documentStorage.store(file.getBytes(), file.getOriginalFilename());
        } catch (IOException e) {
            throw new BusinessException("FILE_READ_FAILED", "Could not read the uploaded file");
        }
    }

    private DocumentResponse toResponse(Document d) {
        return new DocumentResponse(
                d.getId(), d.getPublicId(), d.getClaimId(), d.getDocumentType(),
                d.getFileName(), d.getContentType(), d.getSizeBytes(), d.getStatus(), d.getCreatedAt());
    }

    private DocumentVersionResponse toVersionResponse(DocumentVersion v, Long currentVersionId) {
        return new DocumentVersionResponse(
                v.getId(), v.getDocumentId(), v.getVersionNumber(), v.getFileName(), v.getContentType(),
                v.getSizeBytes(), v.getStatus(), Objects.equals(v.getId(), currentVersionId), v.getCreatedAt());
    }
}
