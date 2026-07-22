package com.niyotechnologies.claimlens.claim.controller;

import com.niyotechnologies.claimlens.claim.dto.request.AssignClaimRequest;
import com.niyotechnologies.claimlens.claim.dto.request.ClaimDecisionRequest;
import com.niyotechnologies.claimlens.claim.dto.request.CreateClaimRequest;
import com.niyotechnologies.claimlens.claim.dto.request.RequestInformationRequest;
import com.niyotechnologies.claimlens.claim.dto.response.ClaimResponse;
import com.niyotechnologies.claimlens.claim.service.ClaimService;
import com.niyotechnologies.claimlens.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${claimlens.api.base-path}/claims")
@RequiredArgsConstructor
public class ClaimController {

    @Autowired
    private final ClaimService claimService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ClaimResponse> createDraft(@Valid @RequestBody CreateClaimRequest request) {
        return ApiResponse.success(claimService.createDraft(request));
    }

    @PostMapping("/{claimId}/submit")
    public ApiResponse<ClaimResponse> submit(@PathVariable Long claimId) {
        return ApiResponse.success(claimService.submit(claimId));
    }

    @PostMapping("/{claimId}/assign")
    public ApiResponse<ClaimResponse> assign(
            @PathVariable Long claimId,
            @Valid @RequestBody AssignClaimRequest request) {
        return ApiResponse.success(claimService.assign(claimId, request));
    }

    @PostMapping("/{claimId}/auto-assign")
    public ApiResponse<ClaimResponse> autoAssign(@PathVariable Long claimId) {
        return ApiResponse.success(claimService.autoAssign(claimId));
    }

    @PostMapping("/{claimId}/decision")
    public ApiResponse<ClaimResponse> decide(
            @PathVariable Long claimId,
            @Valid @RequestBody ClaimDecisionRequest request) {
        return ApiResponse.success(claimService.decide(claimId, request));
    }

    @PostMapping("/{claimId}/request-information")
    public ApiResponse<ClaimResponse> requestInformation(
            @PathVariable Long claimId,
            @Valid @RequestBody RequestInformationRequest request) {
        return ApiResponse.success(claimService.requestInformation(claimId, request));
    }

    @GetMapping("/{claimId}")
    public ApiResponse<ClaimResponse> getClaim(@PathVariable Long claimId) {
        return ApiResponse.success(claimService.getClaim(claimId));
    }

    @GetMapping
    public ApiResponse<List<ClaimResponse>> getClaims() {
        return ApiResponse.success(claimService.getClaims());
    }
}
