package com.niyotechnologies.claimlens.processing.ocr;

/**
 * Seam to the OCR service. The default {@link NoOpOcrClient} keeps OCR off (tests, and any env where
 * the Python service isn't wired), while {@code HttpOcrClient} calls the real service when
 * {@code claimlens.ocr.enabled=true}. Implementations must never throw — a failed OCR returns
 * {@link OcrExtraction#none()} so the pipeline still settles (the fraud gate waits for terminal, not
 * success).
 */
public interface OcrClient {

    OcrExtraction extract(byte[] content, String fileName, String contentType, String documentType);
}
