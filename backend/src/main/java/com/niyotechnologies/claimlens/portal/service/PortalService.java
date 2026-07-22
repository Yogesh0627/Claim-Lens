package com.niyotechnologies.claimlens.portal.service;

import com.niyotechnologies.claimlens.claim.dto.response.ClaimResponse;
import com.niyotechnologies.claimlens.document.dto.response.DocumentContent;
import com.niyotechnologies.claimlens.document.dto.response.DocumentResponse;
import com.niyotechnologies.claimlens.policy.dto.response.PolicyResponse;
import com.niyotechnologies.claimlens.portal.dto.request.FileClaimRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Customer self-service. Every method is ownership-scoped to the authenticated customer — a second
 * security boundary BELOW the tenant, because two customers share a tenant and @TenantId alone does
 * not separate them. All reads/writes go through findByIdAndCustomerId, so a customer can only ever
 * touch their own claims, policies and documents.
 */
public interface PortalService {

    List<PolicyResponse> myPolicies();

    List<ClaimResponse> myClaims();

    ClaimResponse myClaim(Long claimId);

    ClaimResponse fileClaim(FileClaimRequest request);

    ClaimResponse submitMyClaim(Long claimId);

    DocumentResponse uploadToMyClaim(Long claimId, String documentType, MultipartFile file);

    List<DocumentResponse> myClaimDocuments(Long claimId);

    DocumentContent downloadMyClaimDocument(Long claimId, Long documentId);
}
