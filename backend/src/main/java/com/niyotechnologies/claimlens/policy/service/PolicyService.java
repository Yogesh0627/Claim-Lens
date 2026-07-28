package com.niyotechnologies.claimlens.policy.service;

import com.niyotechnologies.claimlens.policy.dto.request.CreatePolicyRequest;
import com.niyotechnologies.claimlens.policy.dto.response.PolicyResponse;
import com.niyotechnologies.claimlens.common.response.PagedResponse;

import java.util.List;

public interface PolicyService {

    PolicyResponse createPolicy(CreatePolicyRequest request);

    PolicyResponse cancelPolicy(Long policyId);

    PolicyResponse getPolicy(Long policyId);

    /** Paged directory listing for the Policies screen. */
    PagedResponse<PolicyResponse> getPolicies(int page, int size);

    /** All policies (unpaged) — for the policy picker on the new-claim form. */
    List<PolicyResponse> getPolicyOptions();
}
