package com.niyotechnologies.claimlens.document.service;

import com.niyotechnologies.claimlens.document.dto.response.DocumentContent;
import com.niyotechnologies.claimlens.document.dto.response.DocumentResponse;
import com.niyotechnologies.claimlens.document.dto.response.DocumentVersionResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentService {

    DocumentResponse upload(Long claimId, String documentType, MultipartFile file);

    List<DocumentResponse> listForClaim(Long claimId);

    DocumentContent download(Long claimId, Long documentId);

    /** Upload a new revision of an existing document; prior versions are preserved. */
    DocumentResponse uploadNewVersion(Long claimId, Long documentId, MultipartFile file);

    List<DocumentVersionResponse> listVersions(Long claimId, Long documentId);

    DocumentContent downloadVersion(Long claimId, Long documentId, Long versionId);

    /**
     * Internal composition — the upload/list/download bodies WITHOUT the staff @PreAuthorize. Reached
     * only from the customer portal, which authorizes with PORTAL_* and has already verified the claim
     * belongs to the caller. No controller maps to these.
     */
    DocumentResponse uploadInternal(Long claimId, String documentType, MultipartFile file);

    List<DocumentResponse> listForClaimInternal(Long claimId);

    DocumentContent downloadInternal(Long claimId, Long documentId);

    List<DocumentVersionResponse> listVersionsInternal(Long claimId, Long documentId);

    DocumentContent downloadVersionInternal(Long claimId, Long documentId, Long versionId);
}
