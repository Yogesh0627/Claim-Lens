package com.niyotechnologies.claimlens.coverage.controller;

import com.niyotechnologies.claimlens.common.response.ApiResponse;
import com.niyotechnologies.claimlens.coverage.dto.IngestKnowledgeRequest;
import com.niyotechnologies.claimlens.coverage.dto.IngestKnowledgeResponse;
import com.niyotechnologies.claimlens.coverage.service.CoverageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/** Manage the RAG knowledge base for a product version (the policy wording that answers come from). */
@RestController
@RequestMapping("${claimlens.api.base-path}/products/{productId}/versions/{versionId}/knowledge")
@RequiredArgsConstructor
public class ProductKnowledgeController {

    @Autowired
    private final CoverageService coverageService;

    @GetMapping
    public ApiResponse<IngestKnowledgeResponse> status(
            @PathVariable Long productId, @PathVariable Long versionId) {
        return ApiResponse.success(coverageService.status(productId, versionId));
    }

    @PostMapping
    public ApiResponse<IngestKnowledgeResponse> ingest(
            @PathVariable Long productId,
            @PathVariable Long versionId,
            @Valid @RequestBody IngestKnowledgeRequest request) {
        return ApiResponse.success(coverageService.ingest(productId, versionId, request));
    }
}
