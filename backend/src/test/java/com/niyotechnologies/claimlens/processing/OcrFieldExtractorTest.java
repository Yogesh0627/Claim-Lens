package com.niyotechnologies.claimlens.processing;

import com.niyotechnologies.claimlens.processing.ocr.OcrFieldExtractor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** The Java port of the OCR service's field regexes must recognise the same motor-insurance signals. */
class OcrFieldExtractorTest {

    @Test
    void extractsRegistrationNumbersAcrossSeparators() {
        assertThat(OcrFieldExtractor.registrationNumbers("Vehicle Reg. MH 12 AB 1234 on the RC"))
                .containsExactly("MH12AB1234");
        assertThat(OcrFieldExtractor.registrationNumbers("plate mh-12-ab-1234"))
                .containsExactly("MH12AB1234");
        assertThat(OcrFieldExtractor.registrationNumbers("BH plate 22 BH 1234 A"))
                .containsExactly("22BH1234A");
    }

    @Test
    void extractsPolicyNumbers() {
        assertThat(OcrFieldExtractor.policyNumbers("Policy No: DEMO-POL-1 issued"))
                .containsExactly("DEMO-POL-1");
        assertThat(OcrFieldExtractor.policyNumbers("policy number POL/2024/00042"))
                .containsExactly("POL/2024/00042");
    }

    @Test
    void dedupesAndIgnoresNoise() {
        assertThat(OcrFieldExtractor.registrationNumbers("MH12AB1234 and again MH 12 AB 1234"))
                .containsExactly("MH12AB1234");
        assertThat(OcrFieldExtractor.registrationNumbers("no plates here")).isEmpty();
    }
}
