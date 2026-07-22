package com.niyotechnologies.claimlens.processing.ocr;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ports the OCR service's registration/policy-number regexes (ocr-service/app/extraction/fields.py)
 * so the in-JVM {@link GoogleVisionOcrClient} emits the same structured signals the fraud layer
 * expects (e.g. does the RC's registration match the policy's vehicle?). Signals, not ground truth.
 */
public final class OcrFieldExtractor {

    // Indian plates: 2 letters (state), 1-2 digits (RTO), 1-3 letters (series), 4 digits.
    private static final Pattern REG = Pattern.compile(
            "\\b([A-Z]{2})[\\s-]?([0-9]{1,2})[\\s-]?([A-Z]{1,3})[\\s-]?([0-9]{4})\\b",
            Pattern.CASE_INSENSITIVE);
    // BH-series: 2 digits (year) BH 4 digits 1-2 letters.
    private static final Pattern BH = Pattern.compile(
            "\\b([0-9]{2})[\\s-]?BH[\\s-]?([0-9]{4})[\\s-]?([A-Z]{1,2})\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern POLICY = Pattern.compile(
            "policy\\s*(?:no|number|#)?\\.?\\s*[:\\-]?\\s*([A-Z0-9][A-Z0-9\\-/]{4,})",
            Pattern.CASE_INSENSITIVE);

    private OcrFieldExtractor() {
    }

    public static List<String> registrationNumbers(String text) {
        List<String> out = new ArrayList<>();
        Matcher m = REG.matcher(text);
        while (m.find()) {
            out.add((m.group(1) + m.group(2) + m.group(3) + m.group(4)).toUpperCase());
        }
        Matcher b = BH.matcher(text);
        while (b.find()) {
            out.add((b.group(1) + "BH" + b.group(2) + b.group(3)).toUpperCase());
        }
        return dedupe(out);
    }

    public static List<String> policyNumbers(String text) {
        List<String> out = new ArrayList<>();
        Matcher m = POLICY.matcher(text);
        while (m.find()) {
            out.add(m.group(1).toUpperCase());
        }
        return dedupe(out);
    }

    private static List<String> dedupe(List<String> in) {
        return new ArrayList<>(new LinkedHashSet<>(in));
    }
}
