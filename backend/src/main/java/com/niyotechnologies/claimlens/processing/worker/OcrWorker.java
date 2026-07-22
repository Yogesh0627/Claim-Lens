package com.niyotechnologies.claimlens.processing.worker;

import com.niyotechnologies.claimlens.document.repository.DocumentOcrView;
import com.niyotechnologies.claimlens.document.repository.DocumentRepository;
import com.niyotechnologies.claimlens.document.storage.DocumentStorage;
import com.niyotechnologies.claimlens.processing.entity.OcrJob;
import com.niyotechnologies.claimlens.processing.entity.OcrResult;
import com.niyotechnologies.claimlens.processing.enums.JobStatus;
import com.niyotechnologies.claimlens.processing.ocr.OcrClient;
import com.niyotechnologies.claimlens.processing.ocr.OcrExtraction;
import com.niyotechnologies.claimlens.processing.orchestrator.ProcessingOrchestrator;
import com.niyotechnologies.claimlens.processing.repository.OcrJobRepository;
import com.niyotechnologies.claimlens.processing.repository.OcrResultRepository;
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
 * OCR worker. Claims a PENDING ocr_job, OCRs every document on the claim through the {@link OcrClient}
 * (the Python OCR service when enabled; a no-op otherwise), stores each result, then settles the job
 * and drives the fraud gate. A @Scheduled poller would call pollOnce() in production; tests call it
 * directly. Never lets a single unreadable document fail the whole job.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OcrWorker {

    @Autowired
    private final OcrJobRepository ocrJobRepository;
    @Autowired
    private final ProcessingOrchestrator orchestrator;
    @Autowired
    private final DocumentRepository documentRepository;
    @Autowired
    private final DocumentStorage documentStorage;
    @Autowired
    private final OcrResultRepository ocrResultRepository;
    @Autowired
    private final OcrClient ocrClient;

    /** @return true if a job was claimed and processed, false if the queue was empty. */
    @Transactional
    public boolean pollOnce() {
        List<OcrJob> claimed = ocrJobRepository.findClaimable(JobStatus.PENDING, PageRequest.of(0, 1));
        if (claimed.isEmpty()) {
            return false;
        }
        OcrJob job = claimed.get(0);
        job.setStatus(JobStatus.PROCESSING);
        job.setLockedAt(Instant.now());
        job.setAttemptCount(job.getAttemptCount() + 1);
        ocrJobRepository.saveAndFlush(job);

        ocrClaimDocuments(job.getClaimId(), job.getTenantId());

        job.setStatus(JobStatus.COMPLETE);
        ocrJobRepository.save(job);
        orchestrator.markOcrComplete(job.getClaimId());
        return true;
    }

    private void ocrClaimDocuments(Long claimId, Long tenantId) {
        // Native fetch: bypasses @TenantId since the worker has no request tenant context.
        List<DocumentOcrView> documents = documentRepository.findForOcrByClaimId(claimId);
        for (DocumentOcrView doc : documents) {
            byte[] content;
            try {
                content = documentStorage.retrieve(doc.getStorageKey());
            } catch (Exception ex) {
                log.warn("Skipping OCR for document {} on claim {}: {}", doc.getId(), claimId, ex.getMessage());
                continue;
            }

            OcrExtraction extraction = ocrClient.extract(
                    content, doc.getFileName(), doc.getContentType(), doc.getDocumentType());
            if (extraction.empty()) {
                continue;
            }
            ocrResultRepository.save(toResult(extraction, claimId, tenantId, doc.getId()));
        }
    }

    private OcrResult toResult(OcrExtraction extraction, Long claimId, Long tenantId, Long documentId) {
        OcrResult result = new OcrResult();
        result.setTenantId(tenantId);
        result.setClaimId(claimId);
        result.setDocumentId(documentId);
        result.setEngine(extraction.engine());
        result.setExtractedText(extraction.text());
        if (extraction.confidence() != null) {
            result.setConfidence(BigDecimal.valueOf(extraction.confidence()));
        }
        result.setRegistrationNumbers(join(extraction.registrationNumbers()));
        result.setPolicyNumbers(join(extraction.policyNumbers()));
        return result;
    }

    private static String join(List<String> values) {
        return values == null || values.isEmpty() ? null : String.join(",", values);
    }
}
