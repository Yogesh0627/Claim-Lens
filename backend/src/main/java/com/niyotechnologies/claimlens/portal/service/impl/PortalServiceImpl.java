package com.niyotechnologies.claimlens.portal.service.impl;

import com.niyotechnologies.claimlens.audit.annotation.Auditable;
import com.niyotechnologies.claimlens.claim.dto.request.CreateClaimRequest;
import com.niyotechnologies.claimlens.claim.dto.response.ClaimResponse;
import com.niyotechnologies.claimlens.claim.entity.Claim;
import com.niyotechnologies.claimlens.claim.mapper.ClaimMapper;
import com.niyotechnologies.claimlens.claim.repository.ClaimRepository;
import com.niyotechnologies.claimlens.claim.service.ClaimService;
import com.niyotechnologies.claimlens.common.exception.NotFoundException;
import com.niyotechnologies.claimlens.common.exception.UnauthorizedException;
import com.niyotechnologies.claimlens.document.dto.response.DocumentContent;
import com.niyotechnologies.claimlens.document.dto.response.DocumentResponse;
import com.niyotechnologies.claimlens.document.service.DocumentService;
import com.niyotechnologies.claimlens.policy.dto.response.PolicyResponse;
import com.niyotechnologies.claimlens.policy.entity.InsurancePolicy;
import com.niyotechnologies.claimlens.policy.entity.InsuredVehicle;
import com.niyotechnologies.claimlens.policy.mapper.PolicyMapper;
import com.niyotechnologies.claimlens.policy.repository.InsurancePolicyRepository;
import com.niyotechnologies.claimlens.policy.repository.InsuredVehicleRepository;
import com.niyotechnologies.claimlens.portal.dto.request.FileClaimRequest;
import com.niyotechnologies.claimlens.portal.service.PortalService;
import com.niyotechnologies.claimlens.security.model.ClaimLensPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PortalServiceImpl implements PortalService {

    @Autowired
    private final ClaimService claimService;
    @Autowired
    private final DocumentService documentService;
    @Autowired
    private final ClaimRepository claimRepository;
    @Autowired
    private final ClaimMapper claimMapper;
    @Autowired
    private final InsurancePolicyRepository policyRepository;
    @Autowired
    private final InsuredVehicleRepository vehicleRepository;
    @Autowired
    private final PolicyMapper policyMapper;

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PORTAL_POLICY_READ')")
    public List<PolicyResponse> myPolicies() {
        Long customerId = currentCustomerId();
        return policyRepository.findAllByCustomerIdAndIsDeletedFalse(customerId).stream()
                .map(p -> policyMapper.toResponse(p, vehicleFor(p.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PORTAL_CLAIM_READ')")
    public List<ClaimResponse> myClaims() {
        Long customerId = currentCustomerId();
        return claimRepository.findAllByCustomerIdAndIsDeletedFalseOrderByIdDesc(customerId).stream()
                .map(c -> claimMapper.toResponse(c, List.of()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PORTAL_CLAIM_READ')")
    public ClaimResponse myClaim(Long claimId) {
        return claimMapper.toResponse(ownedClaimOrThrow(claimId), List.of());
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('PORTAL_CLAIM_WRITE')")
    public ClaimResponse fileClaim(FileClaimRequest request) {
        Long customerId = currentCustomerId();

        // The policy must belong to THIS customer — not just this tenant. 404 (never 403) so a
        // customer can't probe for the existence of another customer's policy id.
        InsurancePolicy policy = policyRepository.findByIdAndIsDeletedFalse(request.insurancePolicyId())
                .filter(p -> customerId.equals(p.getCustomerId()))
                .orElseThrow(() -> new NotFoundException("POLICY_NOT_FOUND", "Policy not found"));

        CreateClaimRequest internal = new CreateClaimRequest(
                customerId,                       // forced from the principal, never from the body
                policy.getId(),
                request.incidentDate(),
                request.claimAmount(),
                request.vehicleRegistrationNumber(),
                request.description());
        return claimService.createDraftInternal(internal);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('PORTAL_CLAIM_WRITE')")
    @Auditable(action = "CLAIM_SUBMITTED", entityType = "CLAIM")
    public ClaimResponse submitMyClaim(Long claimId) {
        ownedClaimOrThrow(claimId);
        return claimService.submitInternal(claimId);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('PORTAL_CLAIM_WRITE')")
    public DocumentResponse uploadToMyClaim(Long claimId, String documentType, MultipartFile file) {
        ownedClaimOrThrow(claimId);
        DocumentResponse response = documentService.uploadInternal(claimId, documentType, file);
        // If this upload answers an open information request, re-open the claim + reprocess.
        claimService.recordCustomerResponseInternal(claimId);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PORTAL_CLAIM_READ')")
    public List<DocumentResponse> myClaimDocuments(Long claimId) {
        ownedClaimOrThrow(claimId);
        return documentService.listForClaimInternal(claimId);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PORTAL_CLAIM_READ')")
    public DocumentContent downloadMyClaimDocument(Long claimId, Long documentId) {
        ownedClaimOrThrow(claimId);
        return documentService.downloadInternal(claimId, documentId);
    }

    /**
     * The ownership gate. Loads a claim by (id, currentCustomerId) — so a claim in the same tenant
     * but owned by another customer simply isn't found (404), exactly like the cross-tenant case.
     */
    private Claim ownedClaimOrThrow(Long claimId) {
        return claimRepository.findByIdAndCustomerIdAndIsDeletedFalse(claimId, currentCustomerId())
                .orElseThrow(() -> new NotFoundException("CLAIM_NOT_FOUND", "Claim not found"));
    }

    private InsuredVehicle vehicleFor(Long policyId) {
        return vehicleRepository.findAllByInsurancePolicyIdAndIsDeletedFalse(policyId)
                .stream().findFirst().orElse(null);
    }

    /** The authenticated customer's id, from the principal. A staff login has none → forbidden. */
    private Long currentCustomerId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.getPrincipal() instanceof ClaimLensPrincipal principal
                && principal.customerId() != null) {
            return principal.customerId();
        }
        throw new UnauthorizedException("NOT_A_CUSTOMER", "No customer is bound to this session");
    }
}
