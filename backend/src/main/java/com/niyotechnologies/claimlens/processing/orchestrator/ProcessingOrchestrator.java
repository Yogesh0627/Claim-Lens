package com.niyotechnologies.claimlens.processing.orchestrator;

import com.niyotechnologies.claimlens.claim.entity.Claim;
import com.niyotechnologies.claimlens.processing.entity.AnalysisJob;
import com.niyotechnologies.claimlens.processing.entity.ClaimProcessingState;
import com.niyotechnologies.claimlens.processing.entity.FraudJob;
import com.niyotechnologies.claimlens.processing.entity.OcrJob;
import com.niyotechnologies.claimlens.processing.entity.ProcessingJob;
import com.niyotechnologies.claimlens.processing.enums.JobStatus;
import com.niyotechnologies.claimlens.processing.repository.AnalysisJobRepository;
import com.niyotechnologies.claimlens.processing.repository.ClaimProcessingStateRepository;
import com.niyotechnologies.claimlens.processing.repository.FraudJobRepository;
import com.niyotechnologies.claimlens.processing.repository.OcrJobRepository;
import com.niyotechnologies.claimlens.processing.support.ProcessingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coordinates the async pipeline. On submit it creates the per-claim processing state + the OCR and
 * analysis jobs. As each stage completes it flips that stage's status and attempts the fraud gate:
 * only when BOTH stages are COMPLETE and fraud is NOT_STARTED does exactly one fraud job get queued
 * — enforced by a conditional atomic UPDATE on the single processing-state row.
 */
@Service
@RequiredArgsConstructor
public class ProcessingOrchestrator {

    @Autowired
    private final ClaimProcessingStateRepository stateRepository;
    @Autowired
    private final OcrJobRepository ocrJobRepository;
    @Autowired
    private final AnalysisJobRepository analysisJobRepository;
    @Autowired
    private final FraudJobRepository fraudJobRepository;

    @Transactional
    public void onClaimSubmitted(Claim claim) {
        ClaimProcessingState state = new ClaimProcessingState();
        state.setTenantId(claim.getTenantId());
        state.setClaimId(claim.getId());
        state.setOcrStatus(ProcessingStatus.PENDING);
        state.setAnalysisStatus(ProcessingStatus.PENDING);
        state.setFraudStatus(ProcessingStatus.NOT_STARTED);
        stateRepository.save(state);

        ocrJobRepository.save(newJob(new OcrJob(), claim));
        analysisJobRepository.save(newJob(new AnalysisJob(), claim));
    }

    /**
     * Re-run the pipeline after the customer answers an information request. Resets the existing
     * per-claim state and re-queues OCR + analysis so the new document is processed and fraud
     * re-evaluates over the full, updated document set (the workers are claim-scoped). Falls back to
     * a fresh submit if — unexpectedly — there is no prior state.
     */
    @Transactional
    public void onCustomerResponse(Claim claim) {
        ClaimProcessingState state = stateRepository.findByClaimId(claim.getId()).orElse(null);
        if (state == null) {
            onClaimSubmitted(claim);
            return;
        }
        state.setOcrStatus(ProcessingStatus.PENDING);
        state.setAnalysisStatus(ProcessingStatus.PENDING);
        state.setFraudStatus(ProcessingStatus.NOT_STARTED);
        state.setPendingReprocess(false);
        stateRepository.save(state);

        ocrJobRepository.save(newJob(new OcrJob(), claim));
        analysisJobRepository.save(newJob(new AnalysisJob(), claim));
    }

    @Transactional
    public void markOcrComplete(Long claimId) {
        stateRepository.findByClaimId(claimId).ifPresent(state -> {
            state.setOcrStatus(ProcessingStatus.COMPLETE);
            stateRepository.saveAndFlush(state);
            tryQueueFraud(claimId, state.getTenantId());
        });
    }

    @Transactional
    public void markAnalysisComplete(Long claimId) {
        stateRepository.findByClaimId(claimId).ifPresent(state -> {
            state.setAnalysisStatus(ProcessingStatus.COMPLETE);
            stateRepository.saveAndFlush(state);
            tryQueueFraud(claimId, state.getTenantId());
        });
    }

    @Transactional
    public void markFraudComplete(Long claimId) {
        stateRepository.findByClaimId(claimId).ifPresent(state -> {
            state.setFraudStatus(ProcessingStatus.COMPLETE);
            stateRepository.save(state);
        });
    }

    /** The gate: at most one fraud job per claim. */
    @Transactional
    public void tryQueueFraud(Long claimId, Long tenantId) {
        int updated = stateRepository.tryQueueFraud(claimId);
        if (updated == 1) {
            FraudJob job = new FraudJob();
            job.setTenantId(tenantId);
            job.setClaimId(claimId);
            job.setStatus(JobStatus.PENDING);
            fraudJobRepository.save(job);
        }
    }

    private <T extends ProcessingJob> T newJob(T job, Claim claim) {
        job.setTenantId(claim.getTenantId());
        job.setClaimId(claim.getId());
        job.setStatus(JobStatus.PENDING);
        return job;
    }
}
