package com.niyotechnologies.claimlens.claim.service;

import com.niyotechnologies.claimlens.claim.dto.request.AssignClaimRequest;
import com.niyotechnologies.claimlens.claim.dto.request.ClaimDecisionRequest;
import com.niyotechnologies.claimlens.claim.dto.request.CreateClaimRequest;
import com.niyotechnologies.claimlens.claim.dto.request.ReassignClaimRequest;
import com.niyotechnologies.claimlens.claim.dto.request.RequestInformationRequest;
import com.niyotechnologies.claimlens.claim.dto.response.ClaimResponse;
import com.niyotechnologies.claimlens.common.response.PagedResponse;

public interface ClaimService {

    ClaimResponse createDraft(CreateClaimRequest request);

    ClaimResponse submit(Long claimId);

    /**
     * Internal composition — the create/submit bodies WITHOUT the staff @PreAuthorize. Reached only
     * from the customer portal (PortalService), which authorizes with PORTAL_* and enforces
     * ownership before calling. No controller maps to these, so they are not a public surface.
     */
    ClaimResponse createDraftInternal(CreateClaimRequest request);

    ClaimResponse submitInternal(Long claimId);

    ClaimResponse assign(Long claimId, AssignClaimRequest request);

    ClaimResponse autoAssign(Long claimId);

    /** Move a claim already under investigation to a different investigator (null id = auto-pick). */
    ClaimResponse reassign(Long claimId, ReassignClaimRequest request);

    ClaimResponse decide(Long claimId, ClaimDecisionRequest request);

    /** Investigator asks the policyholder for more info → claim moves to WAITING_FOR_CUSTOMER. */
    ClaimResponse requestInformation(Long claimId, RequestInformationRequest request);

    /**
     * Internal — called by the portal after a customer uploads a document. If the claim was
     * WAITING_FOR_CUSTOMER it returns to UNDER_INVESTIGATION, the pipeline re-runs, and the assigned
     * investigator is notified. A no-op for any other status (e.g. a draft upload). No @PreAuthorize;
     * the portal has already enforced ownership.
     */
    ClaimResponse recordCustomerResponseInternal(Long claimId);

    ClaimResponse getClaim(Long claimId);

    PagedResponse<ClaimResponse> getClaims(int page, int size);
}
