package com.niyotechnologies.claimlens.processing.ocr;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Calls the Python OCR service over HTTP. Active only when {@code claimlens.ocr.enabled=true}. Never
 * throws: any failure (service down, timeout, bad response) is logged and returns
 * {@link OcrExtraction#none()} so the pipeline settles regardless — OCR is best-effort input, not a
 * gate.
 */
@Component
@ConditionalOnExpression("${claimlens.ocr.enabled:false} and '${claimlens.ocr.provider:http}' != 'vision'")
@Slf4j
public class HttpOcrClient implements OcrClient {

    private final RestClient restClient;
    private final String sharedSecret;

    public HttpOcrClient(
            @Value("${claimlens.ocr.service-url:http://localhost:8000}") String serviceUrl,
            @Value("${claimlens.ocr.shared-secret:}") String sharedSecret) {
        // Buffering factory so the multipart body is actually written (the default JDK-HttpClient
        // factory sends an empty body for Spring multipart). Same fix as HttpAnalysisClient.
        this.restClient = RestClient.builder()
                .baseUrl(serviceUrl)
                .requestFactory(new SimpleClientHttpRequestFactory())
                .build();
        this.sharedSecret = sharedSecret;
    }

    @Override
    public OcrExtraction extract(byte[] content, String fileName, String contentType, String documentType) {
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", filePart(content, fileName, contentType));
            if (documentType != null) {
                body.add("document_type", documentType);
            }

            OcrServiceResponse response = restClient.post()
                    .uri("/ocr")
                    .headers(h -> {
                        if (StringUtils.hasText(sharedSecret)) {
                            h.add("X-OCR-Token", sharedSecret);
                        }
                    })
                    .body(body)
                    .retrieve()
                    .body(OcrServiceResponse.class);

            if (response == null) {
                return OcrExtraction.none();
            }
            String text = response.text() == null ? "" : response.text();
            return new OcrExtraction(
                    response.engine(),
                    text,
                    response.confidence(),
                    response.fields() != null ? nonNull(response.fields().registrationNumbers()) : List.of(),
                    response.fields() != null ? nonNull(response.fields().policyNumbers()) : List.of(),
                    text.isBlank());
        } catch (Exception ex) {
            log.warn("OCR service call failed for document '{}': {}", fileName, ex.getMessage());
            return OcrExtraction.none();
        }
    }

    private static ByteArrayResource filePart(byte[] content, String fileName, String contentType) {
        return new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return fileName != null ? fileName : "document";
            }
        };
    }

    private static List<String> nonNull(List<String> values) {
        return values != null ? values : List.of();
    }

    // Mirrors the OCR service's JSON response (snake_case fields).
    private record OcrServiceResponse(
            String engine,
            @JsonProperty("page_count") Integer pageCount,
            String text,
            Double confidence,
            Fields fields) {
    }

    private record Fields(
            @JsonProperty("registration_numbers") List<String> registrationNumbers,
            @JsonProperty("policy_numbers") List<String> policyNumbers) {
    }
}
