package com.niyotechnologies.claimlens.portal.controller;

import com.niyotechnologies.claimlens.claim.dto.response.ClaimResponse;
import com.niyotechnologies.claimlens.common.response.ApiResponse;
import com.niyotechnologies.claimlens.document.dto.response.DocumentContent;
import com.niyotechnologies.claimlens.document.dto.response.DocumentResponse;
import com.niyotechnologies.claimlens.policy.dto.response.PolicyResponse;
import com.niyotechnologies.claimlens.portal.dto.request.FileClaimRequest;
import com.niyotechnologies.claimlens.portal.service.PortalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Customer self-service surface. Distinct base path from the staff API; every operation is
 * ownership-scoped inside {@link PortalService}. The CUSTOMER role holds only PORTAL_* permissions,
 * so a customer can reach these endpoints and no others.
 */
@RestController
@RequestMapping("${claimlens.api.base-path}/portal")
@RequiredArgsConstructor
public class PortalController {

    @Autowired
    private final PortalService portalService;

    @GetMapping("/policies")
    public ApiResponse<List<PolicyResponse>> myPolicies() {
        return ApiResponse.success(portalService.myPolicies());
    }

    @GetMapping("/claims")
    public ApiResponse<List<ClaimResponse>> myClaims() {
        return ApiResponse.success(portalService.myClaims());
    }

    @GetMapping("/claims/{claimId}")
    public ApiResponse<ClaimResponse> myClaim(@PathVariable Long claimId) {
        return ApiResponse.success(portalService.myClaim(claimId));
    }

    @PostMapping("/claims")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ClaimResponse> fileClaim(@Valid @RequestBody FileClaimRequest request) {
        return ApiResponse.success(portalService.fileClaim(request));
    }

    @PostMapping("/claims/{claimId}/submit")
    public ApiResponse<ClaimResponse> submitMyClaim(@PathVariable Long claimId) {
        return ApiResponse.success(portalService.submitMyClaim(claimId));
    }

    @PostMapping(value = "/claims/{claimId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DocumentResponse> uploadDocument(
            @PathVariable Long claimId,
            @RequestParam("documentType") String documentType,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(portalService.uploadToMyClaim(claimId, documentType, file));
    }

    @GetMapping("/claims/{claimId}/documents")
    public ApiResponse<List<DocumentResponse>> myClaimDocuments(@PathVariable Long claimId) {
        return ApiResponse.success(portalService.myClaimDocuments(claimId));
    }

    @GetMapping("/claims/{claimId}/documents/{documentId}/download")
    public ResponseEntity<byte[]> download(
            @PathVariable Long claimId,
            @PathVariable Long documentId) {
        DocumentContent doc = portalService.downloadMyClaimDocument(claimId, documentId);
        MediaType contentType = doc.contentType() != null
                ? MediaType.parseMediaType(doc.contentType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(doc.fileName()).build().toString())
                .body(doc.content());
    }
}
