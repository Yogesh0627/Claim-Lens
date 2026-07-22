package com.niyotechnologies.claimlens.processing.controller;

import com.niyotechnologies.claimlens.common.response.ApiResponse;
import com.niyotechnologies.claimlens.processing.dto.ClaimProcessingResponse;
import com.niyotechnologies.claimlens.processing.service.ProcessingQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${claimlens.api.base-path}/claims/{claimId}/processing")
@RequiredArgsConstructor
public class ProcessingController {

    @Autowired
    private final ProcessingQueryService processingQueryService;

    @GetMapping
    public ApiResponse<ClaimProcessingResponse> get(@PathVariable Long claimId) {
        return ApiResponse.success(processingQueryService.getForClaim(claimId));
    }
}
