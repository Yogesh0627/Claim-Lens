package com.niyotechnologies.claimlens.processing.worker;

import com.niyotechnologies.claimlens.fraud.engine.FraudEngine;
import com.niyotechnologies.claimlens.processing.entity.FraudJob;
import com.niyotechnologies.claimlens.processing.enums.JobStatus;
import com.niyotechnologies.claimlens.processing.orchestrator.ProcessingOrchestrator;
import com.niyotechnologies.claimlens.processing.repository.FraudJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * The two transactional halves of fraud processing, in a separate bean so the calls are proxied
 * (self-invocation would bypass @Transactional). claimNext commits the PROCESSING mark; the caller
 * then binds the tenant; process runs in a fresh transaction whose session sees that tenant.
 */
@Service
@RequiredArgsConstructor
public class FraudJobProcessor {

    @Autowired
    private final FraudJobRepository fraudJobRepository;
    @Autowired
    private final ProcessingOrchestrator orchestrator;
    @Autowired
    private final FraudEngine fraudEngine;

    /** @return {jobId, claimId, tenantId} or null if no job was claimable. */
    @Transactional
    public long[] claimNext() {
        List<FraudJob> claimed = fraudJobRepository.findClaimable(JobStatus.PENDING, PageRequest.of(0, 1));
        if (claimed.isEmpty()) {
            return null;
        }
        FraudJob job = claimed.get(0);
        job.setStatus(JobStatus.PROCESSING);
        job.setLockedAt(Instant.now());
        job.setAttemptCount(job.getAttemptCount() + 1);
        fraudJobRepository.saveAndFlush(job);
        return new long[]{job.getId(), job.getClaimId(), job.getTenantId()};
    }

    @Transactional
    public void process(long jobId, long claimId) {
        fraudEngine.evaluate(claimId);
        fraudJobRepository.findById(jobId).ifPresent(job -> {
            job.setStatus(JobStatus.COMPLETE);
            fraudJobRepository.save(job);
        });
        orchestrator.markFraudComplete(claimId);
    }
}
