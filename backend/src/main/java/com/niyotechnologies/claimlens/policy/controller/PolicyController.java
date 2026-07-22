package com.niyotechnologies.claimlens.policy.controller;

import com.niyotechnologies.claimlens.common.response.ApiResponse;
import com.niyotechnologies.claimlens.policy.dto.request.CreatePolicyRequest;
import com.niyotechnologies.claimlens.policy.dto.response.PolicyResponse;
import com.niyotechnologies.claimlens.policy.service.PolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${claimlens.api.base-path}/policies")
@RequiredArgsConstructor
public class PolicyController {

    @Autowired
    private final PolicyService policyService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PolicyResponse> createPolicy(@Valid @RequestBody CreatePolicyRequest request) {
        return ApiResponse.success(policyService.createPolicy(request));
    }

    @GetMapping("/{policyId}")
    public ApiResponse<PolicyResponse> getPolicy(@PathVariable Long policyId) {
        return ApiResponse.success(policyService.getPolicy(policyId));
    }

    @GetMapping
    public ApiResponse<List<PolicyResponse>> getPolicies() {
        return ApiResponse.success(policyService.getPolicies());
    }

    @PostMapping("/{policyId}/cancel")
    public ApiResponse<PolicyResponse> cancelPolicy(@PathVariable Long policyId) {
        return ApiResponse.success(policyService.cancelPolicy(policyId));
    }
}
