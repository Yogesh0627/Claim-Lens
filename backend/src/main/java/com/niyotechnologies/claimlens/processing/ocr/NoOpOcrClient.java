package com.niyotechnologies.claimlens.processing.ocr;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default OCR client: does nothing. Active unless {@code claimlens.ocr.enabled=true}, so tests and
 * un-wired environments run the pipeline exactly as before (OCR simply produces no text).
 */
@Component
@ConditionalOnProperty(name = "claimlens.ocr.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpOcrClient implements OcrClient {

    @Override
    public OcrExtraction extract(byte[] content, String fileName, String contentType, String documentType) {
        return OcrExtraction.none();
    }
}
