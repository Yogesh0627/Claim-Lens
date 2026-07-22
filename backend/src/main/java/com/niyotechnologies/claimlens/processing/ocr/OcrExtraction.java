package com.niyotechnologies.claimlens.processing.ocr;

import java.util.List;

/**
 * The result of OCR'ing one document: the raw text plus the best-effort structured signals the OCR
 * service parsed out. These are hints for the fraud/investigation layers, never authoritative values.
 */
public record OcrExtraction(
        String engine,
        String text,
        Double confidence,
        List<String> registrationNumbers,
        List<String> policyNumbers,
        boolean empty
) {

    public static OcrExtraction none() {
        return new OcrExtraction("none", "", null, List.of(), List.of(), true);
    }
}
