package com.niyotechnologies.claimlens.processing.worker;

import com.niyotechnologies.claimlens.document.repository.DocumentOcrView;
import com.niyotechnologies.claimlens.document.repository.DocumentRepository;
import com.niyotechnologies.claimlens.document.storage.DocumentStorage;
import com.niyotechnologies.claimlens.processing.analysis.AnalysisClient;
import com.niyotechnologies.claimlens.processing.analysis.AnalysisSignals;
import com.niyotechnologies.claimlens.processing.entity.AnalysisJob;
import com.niyotechnologies.claimlens.processing.entity.AnalysisResult;
import com.niyotechnologies.claimlens.processing.enums.JobStatus;
import com.niyotechnologies.claimlens.processing.orchestrator.ProcessingOrchestrator;
import com.niyotechnologies.claimlens.processing.repository.AnalysisJobRepository;
import com.niyotechnologies.claimlens.processing.repository.AnalysisResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Image-analysis worker. Claims an analysis_job, runs every image on the claim through the
 * {@link AnalysisClient} (the Python analysis service when enabled; a no-op otherwise), stores the
 * signals — including a cross-claim perceptual-hash duplicate flag — then settles and drives the fraud
 * gate. Non-images are skipped; a single unreadable image never fails the job.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnalysisWorker {

    @Autowired
    private final AnalysisJobRepository analysisJobRepository;
    @Autowired
    private final ProcessingOrchestrator orchestrator;
    @Autowired
    private final DocumentRepository documentRepository;
    @Autowired
    private final DocumentStorage documentStorage;
    @Autowired
    private final AnalysisResultRepository analysisResultRepository;
    @Autowired
    private final AnalysisClient analysisClient;

    @Transactional
    public boolean pollOnce() {
        List<AnalysisJob> claimed =
                analysisJobRepository.findClaimable(JobStatus.PENDING, PageRequest.of(0, 1));
        if (claimed.isEmpty()) {
            return false;
        }
        AnalysisJob job = claimed.get(0);
        job.setStatus(JobStatus.PROCESSING);
        job.setLockedAt(Instant.now());
        job.setAttemptCount(job.getAttemptCount() + 1);
        analysisJobRepository.saveAndFlush(job);

        analyzeClaimImages(job.getClaimId(), job.getTenantId());

        job.setStatus(JobStatus.COMPLETE);
        analysisJobRepository.save(job);
        orchestrator.markAnalysisComplete(job.getClaimId());
        return true;
    }

    private void analyzeClaimImages(Long claimId, Long tenantId) {
        List<DocumentOcrView> documents = documentRepository.findForOcrByClaimId(claimId);
        for (DocumentOcrView doc : documents) {
            if (!isImage(doc.getContentType())) {
                continue;
            }
            byte[] content;
            try {
                content = documentStorage.retrieve(doc.getStorageKey());
            } catch (Exception ex) {
                log.warn("Skipping analysis for document {} on claim {}: {}",
                        doc.getId(), claimId, ex.getMessage());
                continue;
            }

            AnalysisSignals signals = analysisClient.analyze(content, doc.getFileName(), doc.getContentType());
            if (signals.empty()) {
                continue;
            }
            analysisResultRepository.save(toResult(signals, claimId, tenantId, doc.getId()));
        }
    }

    private AnalysisResult toResult(AnalysisSignals signals, Long claimId, Long tenantId, Long documentId) {
        AnalysisResult result = new AnalysisResult();
        result.setTenantId(tenantId);
        result.setClaimId(claimId);
        result.setDocumentId(documentId);
        result.setPhash(signals.phash());
        result.setDhash(signals.dhash());
        result.setAverageHash(signals.averageHash());
        result.setExifState(signals.exifState());
        result.setSyntheticSignal(signals.syntheticSignal());
        if (signals.syntheticScore() != null) {
            result.setSyntheticScore(BigDecimal.valueOf(signals.syntheticScore()));
        }
        // Photo-reuse signal: same pHash already seen on a different claim in this tenant.
        if (signals.phash() != null) {
            analysisResultRepository
                    .findFirstByTenantIdAndPhashAndClaimIdNot(tenantId, signals.phash(), claimId)
                    .ifPresent(existing -> result.setDuplicateOfClaimId(existing.getClaimId()));
        }
        return result;
    }

    private static boolean isImage(String contentType) {
        return contentType != null && contentType.toLowerCase().startsWith("image/");
    }
}
