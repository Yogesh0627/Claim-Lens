package com.niyotechnologies.claimlens.document.controller;

import com.niyotechnologies.claimlens.common.response.ApiResponse;
import com.niyotechnologies.claimlens.document.dto.response.DocumentContent;
import com.niyotechnologies.claimlens.document.dto.response.DocumentResponse;
import com.niyotechnologies.claimlens.document.dto.response.DocumentVersionResponse;
import com.niyotechnologies.claimlens.document.service.DocumentService;
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

@RestController
@RequestMapping("${claimlens.api.base-path}/claims/{claimId}/documents")
@RequiredArgsConstructor
public class DocumentController {

    @Autowired
    private final DocumentService documentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DocumentResponse> upload(
            @PathVariable Long claimId,
            @RequestParam("documentType") String documentType,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(documentService.upload(claimId, documentType, file));
    }

    @GetMapping
    public ApiResponse<List<DocumentResponse>> list(@PathVariable Long claimId) {
        return ApiResponse.success(documentService.listForClaim(claimId));
    }

    @PostMapping(value = "/{documentId}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DocumentResponse> uploadVersion(
            @PathVariable Long claimId,
            @PathVariable Long documentId,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(documentService.uploadNewVersion(claimId, documentId, file));
    }

    @GetMapping("/{documentId}/versions")
    public ApiResponse<List<DocumentVersionResponse>> listVersions(
            @PathVariable Long claimId,
            @PathVariable Long documentId) {
        return ApiResponse.success(documentService.listVersions(claimId, documentId));
    }

    @GetMapping("/{documentId}/download")
    public ResponseEntity<byte[]> download(
            @PathVariable Long claimId,
            @PathVariable Long documentId) {
        return fileResponse(documentService.download(claimId, documentId));
    }

    @GetMapping("/{documentId}/versions/{versionId}/download")
    public ResponseEntity<byte[]> downloadVersion(
            @PathVariable Long claimId,
            @PathVariable Long documentId,
            @PathVariable Long versionId) {
        return fileResponse(documentService.downloadVersion(claimId, documentId, versionId));
    }

    private ResponseEntity<byte[]> fileResponse(DocumentContent doc) {
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
