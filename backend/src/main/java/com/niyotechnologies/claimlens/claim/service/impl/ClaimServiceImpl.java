package com.niyotechnologies.claimlens.claim.service.impl;

import com.niyotechnologies.claimlens.audit.annotation.Auditable;
import com.niyotechnologies.claimlens.assignment.engine.AssignmentEngine;
import com.niyotechnologies.claimlens.assignment.entity.ClaimAssignment;
import com.niyotechnologies.claimlens.assignment.enums.AssignmentStatus;
import com.niyotechnologies.claimlens.assignment.repository.ClaimAssignmentRepository;
import com.niyotechnologies.claimlens.claim.dto.request.AssignClaimRequest;
import com.niyotechnologies.claimlens.claim.dto.request.ClaimDecisionRequest;
import com.niyotechnologies.claimlens.claim.dto.request.CreateClaimRequest;
import com.niyotechnologies.claimlens.claim.dto.request.RequestInformationRequest;
import com.niyotechnologies.claimlens.claim.dto.response.ClaimResponse;
import com.niyotechnologies.claimlens.claim.entity.Claim;
import com.niyotechnologies.claimlens.claim.entity.ClaimStatusHistory;
import com.niyotechnologies.claimlens.claim.enums.ClaimDecision;
import com.niyotechnologies.claimlens.claim.enums.ClaimStatus;
import com.niyotechnologies.claimlens.claim.mapper.ClaimMapper;
import com.niyotechnologies.claimlens.claim.repository.ClaimRepository;
import com.niyotechnologies.claimlens.claim.repository.ClaimStatusHistoryRepository;
import com.niyotechnologies.claimlens.claim.service.ClaimService;
import com.niyotechnologies.claimlens.claim.validator.ClaimSubmissionValidator;
import com.niyotechnologies.claimlens.common.exception.BusinessException;
import com.niyotechnologies.claimlens.common.exception.NotFoundException;
import com.niyotechnologies.claimlens.customer.repository.CustomerRepository;
import com.niyotechnologies.claimlens.document.entity.Document;
import com.niyotechnologies.claimlens.document.repository.DocumentRepository;
import com.niyotechnologies.claimlens.document.storage.DocumentStorage;
import com.niyotechnologies.claimlens.fraud.entity.FraudScore;
import com.niyotechnologies.claimlens.fraud.repository.FraudScoreRepository;
import com.niyotechnologies.claimlens.notification.report.ClaimEmailEvent;
import com.niyotechnologies.claimlens.notification.report.ClaimReportContext;
import com.niyotechnologies.claimlens.notification.report.ClaimReportContext.DocRef;
import com.niyotechnologies.claimlens.notification.report.ClaimReportContext.StatusTone;
import com.niyotechnologies.claimlens.notification.service.NotificationService;
import com.niyotechnologies.claimlens.security.model.ClaimLensPrincipal;
import com.niyotechnologies.claimlens.policy.entity.InsurancePolicy;
import com.niyotechnologies.claimlens.policy.entity.InsuredVehicle;
import com.niyotechnologies.claimlens.policy.repository.InsurancePolicyRepository;
import com.niyotechnologies.claimlens.policy.repository.InsuredVehicleRepository;
import com.niyotechnologies.claimlens.processing.orchestrator.ProcessingOrchestrator;
import com.niyotechnologies.claimlens.product.entity.InsuranceProduct;
import com.niyotechnologies.claimlens.product.repository.InsuranceProductRepository;
import com.niyotechnologies.claimlens.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Claim intake. createDraft inherits the pinned product version FROM THE POLICY (so the claim is
 * judged against the terms the customer agreed to) and snapshots the policy number. submit runs the
 * hard/soft validations: hard failures reject; soft signals are returned as warnings, never a reject.
 */
@Service
@RequiredArgsConstructor
public class ClaimServiceImpl implements ClaimService {

    @Autowired
    private final ClaimRepository claimRepository;
    @Autowired
    private final ClaimStatusHistoryRepository historyRepository;
    @Autowired
    private final InsurancePolicyRepository policyRepository;
    @Autowired
    private final InsuredVehicleRepository vehicleRepository;
    @Autowired
    private final InsuranceProductRepository productRepository;
    @Autowired
    private final CustomerRepository customerRepository;
    @Autowired
    private final AppUserRepository appUserRepository;
    @Autowired
    private final ClaimAssignmentRepository assignmentRepository;
    @Autowired
    private final AssignmentEngine assignmentEngine;
    @Autowired
    private final NotificationService notificationService;
    @Autowired
    private final ProcessingOrchestrator processingOrchestrator;
    @Autowired
    private final ClaimSubmissionValidator submissionValidator;
    @Autowired
    private final ClaimMapper claimMapper;
    @Autowired
    private final DocumentRepository documentRepository;
    @Autowired
    private final FraudScoreRepository fraudScoreRepository;
    @Autowired
    private final DocumentStorage documentStorage;

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('CLAIM_WRITE')")
    public ClaimResponse createDraft(CreateClaimRequest request) {
        return createDraftInternal(request);
    }

    @Override
    @Transactional
    public ClaimResponse createDraftInternal(CreateClaimRequest request) {
        InsurancePolicy policy = policyRepository.findByIdAndIsDeletedFalse(request.insurancePolicyId())
                .orElseThrow(() -> new NotFoundException("POLICY_NOT_FOUND", "Policy not found"));

        customerRepository.findByIdAndIsDeletedFalse(request.customerId())
                .orElseThrow(() -> new NotFoundException("CUSTOMER_NOT_FOUND", "Customer not found"));

        InsuranceProduct product = productRepository
                .findByIdAndIsDeletedFalse(policy.getInsuranceProductId())
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not found"));

        InsuredVehicle vehicle = vehicleRepository
                .findAllByInsurancePolicyIdAndIsDeletedFalse(policy.getId())
                .stream().findFirst().orElse(null);

        Claim claim = new Claim();
        claim.setClaimNumber(generateClaimNumber());
        claim.setCustomerId(request.customerId());
        claim.setInsurancePolicyId(policy.getId());
        claim.setInsuredVehicleId(vehicle == null ? null : vehicle.getId());
        claim.setInsuranceProductId(policy.getInsuranceProductId());
        // Version pinning: the claim inherits the policy's pinned version, never the product's current one.
        claim.setInsuranceProductVersionId(policy.getInsuranceProductVersionId());
        claim.setClaimTypeId(product.getClaimTypeId());
        claim.setPolicyNumber(policy.getPolicyNumber());               // snapshot
        claim.setVehicleRegistrationNumber(request.vehicleRegistrationNumber());
        claim.setIncidentDate(request.incidentDate());
        claim.setReportedDate(LocalDate.now());
        claim.setClaimAmount(request.claimAmount());
        claim.setDescription(request.description());
        claim.setStatus(ClaimStatus.DRAFT);

        Claim saved = claimRepository.save(claim);
        recordHistory(saved, null, ClaimStatus.DRAFT, "Claim created");
        return claimMapper.toResponse(saved, List.of());
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('CLAIM_SUBMIT')")
    @Auditable(action = "CLAIM_SUBMITTED", entityType = "CLAIM")
    public ClaimResponse submit(Long claimId) {
        return submitInternal(claimId);
    }

    @Override
    @Transactional
    public ClaimResponse submitInternal(Long claimId) {
        Claim claim = getClaimOrThrow(claimId);
        if (claim.getStatus() != ClaimStatus.DRAFT) {
            throw new BusinessException("CLAIM_NOT_DRAFT", "Only a draft claim can be submitted");
        }

        InsurancePolicy policy = policyRepository.findByIdAndIsDeletedFalse(claim.getInsurancePolicyId())
                .orElseThrow(() -> new NotFoundException("POLICY_NOT_FOUND", "Policy not found"));
        InsuredVehicle vehicle = claim.getInsuredVehicleId() == null ? null
                : vehicleRepository.findById(claim.getInsuredVehicleId()).orElse(null);

        // Hard failures throw here; soft signals come back as warnings.
        List<String> warnings = submissionValidator.validate(claim, policy, vehicle);

        // Submit moves straight into background processing (OCR + analysis, then fraud).
        claim.setStatus(ClaimStatus.AWAITING_ANALYSIS);
        claim.setSubmittedAt(Instant.now());
        Claim saved = claimRepository.save(claim);
        recordHistory(saved, ClaimStatus.DRAFT, ClaimStatus.AWAITING_ANALYSIS,
                warnings.isEmpty() ? "Submitted" : "Submitted with signals: " + warnings);
        processingOrchestrator.onClaimSubmitted(saved);

        notifyCustomer(saved, ClaimEmailEvent.SUBMITTED, "CLAIM_SUBMITTED",
                "Claim " + saved.getClaimNumber() + " received",
                "We've received your claim " + saved.getClaimNumber()
                        + " and have started reviewing it. We'll be in touch as it progresses.",
                buildReportContext(saved, null, null, null));

        return claimMapper.toResponse(saved, warnings);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('CLAIM_ASSIGN')")
    @Auditable(action = "CLAIM_ASSIGNED", entityType = "CLAIM")
    public ClaimResponse assign(Long claimId, AssignClaimRequest request) {
        Claim claim = getAssignableClaimOrThrow(claimId);
        appUserRepository.findByIdAndIsDeletedFalse(request.investigatorUserId())
                .orElseThrow(() -> new NotFoundException("INVESTIGATOR_NOT_FOUND", "Investigator not found"));
        return doAssign(claim, request.investigatorUserId());
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('CLAIM_ASSIGN')")
    @Auditable(action = "CLAIM_ASSIGNED", entityType = "CLAIM")
    public ClaimResponse autoAssign(Long claimId) {
        Claim claim = getAssignableClaimOrThrow(claimId);
        // Strategy-driven pick (least-loaded eligible investigator in this tenant).
        Long investigatorId = assignmentEngine.pickInvestigator();
        return doAssign(claim, investigatorId);
    }

    private Claim getAssignableClaimOrThrow(Long claimId) {
        Claim claim = getClaimOrThrow(claimId);
        // After processing + fraud, a claim sits at AWAITING_ASSIGNMENT ready to be assigned.
        if (claim.getStatus() != ClaimStatus.AWAITING_ASSIGNMENT) {
            throw new BusinessException("CLAIM_NOT_ASSIGNABLE",
                    "Only a claim awaiting assignment can be assigned");
        }
        return claim;
    }

    private ClaimResponse doAssign(Claim claim, Long investigatorUserId) {
        ClaimAssignment assignment = new ClaimAssignment();
        assignment.setClaimId(claim.getId());
        assignment.setInvestigatorUserId(investigatorUserId);
        assignment.setStatus(AssignmentStatus.ASSIGNED);
        assignmentRepository.save(assignment);

        claim.setStatus(ClaimStatus.UNDER_INVESTIGATION);
        Claim saved = claimRepository.save(claim);
        recordHistory(saved, ClaimStatus.AWAITING_ASSIGNMENT, ClaimStatus.UNDER_INVESTIGATION,
                "Assigned to investigator " + investigatorUserId);
        String investigatorName = appUserRepository.findByIdAndIsDeletedFalse(investigatorUserId)
                .map(u -> fullName(u.getFirstName(), u.getLastName())).orElse(null);
        notificationService.notifyClaimEvent(investigatorUserId, "CLAIM_ASSIGNED",
                "New claim assigned",
                "Claim " + saved.getClaimNumber() + " has been assigned to you",
                ClaimEmailEvent.ASSIGNED, buildReportContext(saved, investigatorName, null, null));
        return claimMapper.toResponse(saved, List.of());
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('CLAIM_DECIDE')")
    @Auditable(action = "CLAIM_DECIDED", entityType = "CLAIM")
    public ClaimResponse decide(Long claimId, ClaimDecisionRequest request) {
        Claim claim = getClaimOrThrow(claimId);
        if (claim.getStatus() != ClaimStatus.UNDER_INVESTIGATION) {
            throw new BusinessException("CLAIM_NOT_UNDER_INVESTIGATION",
                    "Only a claim under investigation can be decided");
        }
        Instant now = Instant.now();
        ClaimStatus target;
        if (request.decision() == ClaimDecision.APPROVE) {
            claim.setApprovedAt(now);
            target = ClaimStatus.APPROVED;
        } else {
            claim.setRejectedAt(now);
            target = ClaimStatus.REJECTED;
        }
        claim.setStatus(target);
        Claim saved = claimRepository.save(claim);
        recordHistory(saved, ClaimStatus.UNDER_INVESTIGATION, target,
                request.reason() == null ? request.decision().name() : request.reason());

        String outcome = target == ClaimStatus.APPROVED ? "approved" : "not approved";
        String deciderName = currentUserName();
        ClaimEmailEvent event = target == ClaimStatus.APPROVED
                ? ClaimEmailEvent.APPROVED : ClaimEmailEvent.REJECTED;
        notifyCustomer(saved, event, "CLAIM_" + target.name(),
                "Claim " + saved.getClaimNumber() + " " + outcome,
                "Your claim " + saved.getClaimNumber() + " has been " + outcome + "."
                        + (request.reason() == null ? "" : " Note: " + request.reason()),
                buildReportContext(saved, deciderName, deciderName, request.reason()));

        return claimMapper.toResponse(saved, List.of());
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('CLAIM_INVESTIGATE')")
    @Auditable(action = "CLAIM_INFO_REQUESTED", entityType = "CLAIM")
    public ClaimResponse requestInformation(Long claimId, RequestInformationRequest request) {
        Claim claim = getClaimOrThrow(claimId);
        if (claim.getStatus() != ClaimStatus.UNDER_INVESTIGATION) {
            throw new BusinessException("CLAIM_NOT_UNDER_INVESTIGATION",
                    "Information can only be requested while the claim is under investigation");
        }
        claim.setStatus(ClaimStatus.WAITING_FOR_CUSTOMER);
        Claim saved = claimRepository.save(claim);
        recordHistory(saved, ClaimStatus.UNDER_INVESTIGATION, ClaimStatus.WAITING_FOR_CUSTOMER,
                "Information requested: " + request.message());

        // The investigator's message is carried as the "note" in the report, and as the in-app message.
        notifyCustomer(saved, ClaimEmailEvent.INFO_REQUESTED, "CLAIM_INFO_REQUESTED",
                "More information needed for claim " + saved.getClaimNumber(),
                request.message(),
                buildReportContext(saved, currentUserName(), null, request.message()));

        return claimMapper.toResponse(saved, List.of());
    }

    @Override
    @Transactional
    public ClaimResponse recordCustomerResponseInternal(Long claimId) {
        Claim claim = getClaimOrThrow(claimId);
        // Only an answer to an open information request re-opens the claim; other uploads (e.g. while
        // still a draft) are just stored.
        if (claim.getStatus() != ClaimStatus.WAITING_FOR_CUSTOMER) {
            return claimMapper.toResponse(claim, List.of());
        }
        claim.setStatus(ClaimStatus.UNDER_INVESTIGATION);
        Claim saved = claimRepository.save(claim);
        recordHistory(saved, ClaimStatus.WAITING_FOR_CUSTOMER, ClaimStatus.UNDER_INVESTIGATION,
                "Customer responded with new information — reprocessing");
        // Re-run OCR + analysis over the updated document set; fraud re-evaluates via the gate.
        processingOrchestrator.onCustomerResponse(saved);
        notifyAssignedInvestigator(saved);
        return claimMapper.toResponse(saved, List.of());
    }

    /** Notify the currently-assigned investigator (rich email + in-app) that the customer responded. */
    private void notifyAssignedInvestigator(Claim claim) {
        assignmentRepository.findAllByClaimId(claim.getId()).stream()
                .filter(a -> a.getStatus() == AssignmentStatus.ASSIGNED)
                .map(ClaimAssignment::getInvestigatorUserId)
                .findFirst()
                .ifPresent(investigatorId -> {
                    String investigatorName = appUserRepository.findByIdAndIsDeletedFalse(investigatorId)
                            .map(u -> fullName(u.getFirstName(), u.getLastName())).orElse(null);
                    notificationService.notifyClaimEvent(investigatorId, "CLAIM_CUSTOMER_RESPONDED",
                            "Customer responded on claim " + claim.getClaimNumber(),
                            "The policyholder has uploaded new information for claim "
                                    + claim.getClaimNumber() + " — it's back under investigation.",
                            ClaimEmailEvent.CUSTOMER_RESPONDED,
                            buildReportContext(claim, investigatorName, null, null));
                });
    }

    /** Best-effort rich email + in-app update to the policyholder's self-service login, if they have one. */
    private void notifyCustomer(Claim claim, ClaimEmailEvent event, String type, String title,
                                String message, ClaimReportContext ctx) {
        if (claim.getCustomerId() == null) {
            return;
        }
        appUserRepository.findFirstByCustomerIdAndIsDeletedFalse(claim.getCustomerId())
                .ifPresent(user -> notificationService.notifyClaimEvent(
                        user.getId(), type, title, message, event, ctx));
    }

    /**
     * Assembles the report context for the email + PDF from the claim and its related data. Called
     * inside the claim transaction; the heavy work (fetching image bytes, rendering the PDF) is
     * deferred to the email layer and only runs when rich email is enabled.
     */
    private ClaimReportContext buildReportContext(Claim claim, String investigatorName,
                                                  String decidedByName, String decisionReason) {
        String customerName = customerRepository.findByIdAndIsDeletedFalse(claim.getCustomerId())
                .map(c -> fullName(c.getFirstName(), c.getLastName())).orElse(null);
        FraudScore fraud = fraudScoreRepository.findFirstByClaimIdOrderByCreatedAtDesc(claim.getId())
                .orElse(null);
        List<DocRef> documents = documentRepository.findAllByClaimIdAndIsDeletedFalse(claim.getId())
                .stream().map(this::toDocRef).toList();
        return new ClaimReportContext(
                claim.getClaimNumber(), statusLabel(claim.getStatus()), statusTone(claim.getStatus()),
                customerName, claim.getPolicyNumber(), claim.getVehicleRegistrationNumber(),
                claim.getClaimAmount(), claim.getIncidentDate(), claim.getReportedDate(),
                claim.getDescription(),
                investigatorName, decidedByName, decisionReason,
                fraud == null ? null : fraud.getScore(),
                fraud == null ? null : fraud.getRiskLevel(),
                documents);
    }

    private DocRef toDocRef(Document d) {
        return new DocRef(d.getFileName(), d.getDocumentType(), d.getContentType(),
                documentStorage.publicUrl(d.getStorageKey()).orElse(null), d.getStorageKey());
    }

    private String currentUserName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof ClaimLensPrincipal principal) {
            return appUserRepository.findByIdAndIsDeletedFalse(principal.userId())
                    .map(u -> fullName(u.getFirstName(), u.getLastName())).orElse(null);
        }
        return null;
    }

    private static String fullName(String first, String last) {
        String name = ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim();
        return name.isEmpty() ? null : name;
    }

    private static String statusLabel(ClaimStatus status) {
        return switch (status) {
            case DRAFT -> "Draft";
            case SUBMITTED, AWAITING_ANALYSIS -> "Submitted";
            case AWAITING_ASSIGNMENT -> "Awaiting assignment";
            case AWAITING_ACCEPTANCE -> "Awaiting acceptance";
            case UNDER_INVESTIGATION -> "Under investigation";
            case WAITING_FOR_CUSTOMER -> "Waiting for customer";
            case APPROVED -> "Approved";
            case REJECTED -> "Rejected";
            case CLOSED -> "Closed";
            case REOPENED -> "Reopened";
        };
    }

    private static StatusTone statusTone(ClaimStatus status) {
        return switch (status) {
            case APPROVED -> StatusTone.POSITIVE;
            case REJECTED -> StatusTone.NEGATIVE;
            case DRAFT, CLOSED -> StatusTone.NEUTRAL;
            default -> StatusTone.INFO;
        };
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('CLAIM_READ')")
    public ClaimResponse getClaim(Long claimId) {
        return claimMapper.toResponse(getClaimOrThrow(claimId), List.of());
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('CLAIM_READ')")
    public List<ClaimResponse> getClaims() {
        return claimRepository.findAllByIsDeletedFalse().stream()
                .map(c -> claimMapper.toResponse(c, List.of()))
                .toList();
    }

    private Claim getClaimOrThrow(Long claimId) {
        return claimRepository.findByIdAndIsDeletedFalse(claimId)
                .orElseThrow(() -> new NotFoundException("CLAIM_NOT_FOUND", "Claim not found"));
    }

    private void recordHistory(Claim claim, ClaimStatus from, ClaimStatus to, String note) {
        ClaimStatusHistory history = new ClaimStatusHistory();
        history.setTenantId(claim.getTenantId());
        history.setClaimId(claim.getId());
        history.setFromStatus(from == null ? null : from.name());
        history.setToStatus(to.name());
        history.setNote(note);
        historyRepository.save(history);
    }

    private static String generateClaimNumber() {
        return "CLM-" + Long.toString(System.nanoTime(), 36).toUpperCase();
    }
}
