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

        FraudSummary fraud = fraudScoreRepository.findFirstByClaimIdOrderByCreatedAtDesc(claimId)
                .map(f -> new FraudSummary(f.getScore(), f.getRiskLevel(), f.getExplanation()))
                .orElse(null);

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
}
