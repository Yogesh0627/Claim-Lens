package com.niyotechnologies.claimlens.processing.service;

import com.niyotechnologies.claimlens.claim.repository.ClaimRepository;
import com.niyotechnologies.claimlens.common.exception.NotFoundException;
import com.niyotechnologies.claimlens.fraud.repository.FraudScoreRepository;
import com.niyotechnologies.claimlens.processing.dto.AnalysisResultResponse;
import com.niyotechnologies.claimlens.processing.dto.ClaimProcessingResponse;
import com.niyotechnologies.claimlens.processing.dto.ClaimProcessingResponse.FraudSummary;
import com.niyotechnologies.claimlens.processing.dto.ClaimProcessingResponse.ProcessingState;
import com.niyotechnologies.claimlens.processing.dto.OcrResultResponse;
import com.niyotechnologies.claimlens.processing.repository.AnalysisResultRepository;
import com.niyotechnologies.claimlens.processing.repository.ClaimProcessingStateRepository;
import com.niyotechnologies.claimlens.processing.repository.OcrResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Read side of the pipeline: exposes stored OCR text, image-analysis signals, pipeline state and the
 * fraud score for a claim, so investigators can see what the automation prepared. */
@Service
@RequiredArgsConstructor
public class ProcessingQueryService {

    @Autowired
    private final ClaimRepository claimRepository;
    @Autowired
    private final ClaimProcessingStateRepository stateRepository;
    @Autowired
    private final OcrResultRepository ocrResultRepository;
    @Autowired
    private final AnalysisResultRepository analysisResultRepository;
    @Autowired
    private final FraudScoreRepository fraudScoreRepository;

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('CLAIM_READ')")
    public ClaimProcessingResponse getForClaim(Long claimId) {
        // Tenant-scoped verification (@TenantId): another tenant's claim is a 404, never their data.
        claimRepository.findByIdAndIsDeletedFalse(claimId)
                .orElseThrow(() -> new NotFoundException("CLAIM_NOT_FOUND", "Claim not found"));

        ProcessingState state = stateRepository.findByClaimId(claimId)
                .map(s -> new ProcessingState(s.getOcrStatus(), s.getAnalysisStatus(), s.getFraudStatus()))
                .orElse(null);

        // FRAUD_READ gates the fraud block specifically. Processing STATUS (OCR/analysis progress) is
        // fine for anyone who can read the claim, but the score/risk/explanation is a sensitive
        // assessment restricted by design to the investigation roles — the endpoint is CLAIM_READ, so
        // without this check a role deliberately denied FRAUD_READ (e.g. Customer Support) would still
        // see it. FRAUD_READ was otherwise enforced nowhere: a dead permission until now.
        FraudSummary fraud = hasAuthority("FRAUD_READ")
                ? fraudScoreRepository.findFirstByClaimIdOrderByCreatedAtDesc(claimId)
                        .map(f -> new FraudSummary(f.getScore(), f.getRiskLevel(), f.getExplanation()))
                        .orElse(null)
                : null;

        List<OcrResultResponse> ocr = ocrResultRepository.findAllByClaimIdOrderByCreatedAtAsc(claimId).stream()
                .map(r -> new OcrResultResponse(r.getId(), r.getDocumentId(), r.getEngine(),
                        r.getExtractedText(), r.getConfidence(), r.getRegistrationNumbers(),
                        r.getPolicyNumbers(), r.getCreatedAt()))
                .toList();

        List<AnalysisResultResponse> analysis =
                analysisResultRepository.findAllByClaimIdOrderByCreatedAtAsc(claimId).stream()
                        .map(a -> new AnalysisResultResponse(a.getId(), a.getDocumentId(), a.getPhash(),
                                a.getExifState(), Boolean.TRUE.equals(a.getSyntheticSignal()),
                                a.getSyntheticScore(), a.getDuplicateOfClaimId(), a.getCreatedAt()))
                        .toList();

        return new ClaimProcessingResponse(state, fraud, ocr, analysis);
    }

    /** True when the authenticated caller holds the given permission (a JWT authority). */
    private boolean hasAuthority(String permission) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        for (GrantedAuthority granted : auth.getAuthorities()) {
            if (permission.equals(granted.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
