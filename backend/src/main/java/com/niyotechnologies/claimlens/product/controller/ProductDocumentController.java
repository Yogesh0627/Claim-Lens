package com.niyotechnologies.claimlens.product.controller;

import com.niyotechnologies.claimlens.common.response.ApiResponse;
import com.niyotechnologies.claimlens.common.util.SafeDownloads;
import com.niyotechnologies.claimlens.product.dto.response.ProductDocumentResponse;
import com.niyotechnologies.claimlens.product.service.ProductDocumentService;
import com.niyotechnologies.claimlens.product.service.ProductDocumentService.DownloadedFile;
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

/** Documents attached to a product version (e.g. the policy-wording PDF). */
@RestController
@RequestMapping("${claimlens.api.base-path}/products/{productId}/versions/{versionId}/documents")
@RequiredArgsConstructor
public class ProductDocumentController {

    @Autowired
    private final ProductDocumentService documentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductDocumentResponse> upload(
            @PathVariable Long productId,
            @PathVariable Long versionId,
            @RequestParam(value = "documentType", required = false) String documentType,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(documentService.upload(productId, versionId, file, documentType));
    }

    @GetMapping
    public ApiResponse<List<ProductDocumentResponse>> list(
            @PathVariable Long productId,
            @PathVariable Long versionId) {
        return ApiResponse.success(documentService.listForVersion(productId, versionId));
    }

    @GetMapping("/{documentId}/download")
    public ResponseEntity<byte[]> download(
            @PathVariable Long productId,
            @PathVariable Long versionId,
            @PathVariable Long documentId) {
        DownloadedFile doc = documentService.download(documentId);
        return SafeDownloads.of(doc.content(), doc.fileName(), doc.contentType());
    }
}
