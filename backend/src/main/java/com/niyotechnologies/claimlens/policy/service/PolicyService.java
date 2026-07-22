package com.niyotechnologies.claimlens.policy.service;

import com.niyotechnologies.claimlens.policy.dto.request.CreatePolicyRequest;
import com.niyotechnologies.claimlens.policy.dto.response.PolicyResponse;

import java.util.List;

public interface PolicyService {

    PolicyResponse createPolicy(CreatePolicyRequest request);

    PolicyResponse cancelPolicy(Long policyId);

    PolicyResponse getPolicy(Long policyId);

    List<PolicyResponse> getPolicies();
}
