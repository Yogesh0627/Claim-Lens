package com.niyotechnologies.claimlens.processing.ocr;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * OCR via the Google Cloud Vision REST API using an API key (images:annotate for images,
 * files:annotate for PDFs). Active when {@code claimlens.ocr.enabled=true} AND
 * {@code claimlens.ocr.provider=vision}. Never throws: any failure returns {@link OcrExtraction#none()}
 * so the pipeline still settles (OCR is best-effort input, not a gate). Image PII leaves your infra
 * when this engine is active — a deliberate, documented trade-off.
 */
@Component
@ConditionalOnExpression("${claimlens.ocr.enabled:false} and '${claimlens.ocr.provider:http}' == 'vision'")
@Slf4j
public class GoogleVisionOcrClient implements OcrClient {

    private final RestClient restClient;
    private final String apiKey;

    public GoogleVisionOcrClient(
            @Value("${claimlens.ocr.vision.base-url:https://vision.googleapis.com/v1}") String baseUrl,
            @Value("${claimlens.ocr.vision.api-key:}") String apiKey) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    @Override
    public OcrExtraction extract(byte[] content, String fileName, String contentType, String documentType) {
        try {
            boolean pdf = (contentType != null && contentType.toLowerCase().contains("pdf"))
                    || (fileName != null && fileName.toLowerCase().endsWith(".pdf"));
            String b64 = Base64.getEncoder().encodeToString(content);
            Map<String, Object> feature = Map.of("type", "DOCUMENT_TEXT_DETECTION");

            String path;
            Map<String, Object> body;
            if (pdf) {
                path = "/files:annotate";
                body = Map.of("requests", List.of(Map.of(
                        "inputConfig", Map.of("content", b64, "mimeType", "application/pdf"),
                        "features", List.of(feature),
                        "pages", List.of(1, 2, 3, 4, 5))));
            } else {
                path = "/images:annotate";
                body = Map.of("requests", List.of(Map.of(
                        "image", Map.of("content", b64),
                        "features", List.of(feature))));
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(uri -> uri.path(path).queryParam("key", apiKey).build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            String text = pdf ? textFromFiles(response) : textFromImages(response);
            if (text == null) {
                text = "";
            }
            return new OcrExtraction(
                    "vision",
                    text,
                    null,
                    OcrFieldExtractor.registrationNumbers(text),
                    OcrFieldExtractor.policyNumbers(text),
                    text.isBlank());
        } catch (Exception ex) {
            log.warn("Google Vision OCR failed for document '{}': {}", fileName, ex.getMessage());
            return OcrExtraction.none();
        }
    }

    /** images:annotate → responses[0].fullTextAnnotation.text */
    @SuppressWarnings("unchecked")
    private static String textFromImages(Map<String, Object> response) {
        Map<String, Object> first = firstResponse(response);
        if (first == null) {
            return null;
        }
        checkError(first);
        Map<String, Object> annotation = (Map<String, Object>) first.get("fullTextAnnotation");
        return annotation == null ? null : (String) annotation.get("text");
    }

    /** files:annotate → responses[0].responses[i].fullTextAnnotation.text (one per page). */
    @SuppressWarnings("unchecked")
    private static String textFromFiles(Map<String, Object> response) {
        Map<String, Object> file = firstResponse(response);
        if (file == null) {
            return null;
        }
        List<Map<String, Object>> pages = (List<Map<String, Object>>) file.get("responses");
        if (pages == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> page : pages) {
            checkError(page);
            Map<String, Object> annotation = (Map<String, Object>) page.get("fullTextAnnotation");
            if (annotation != null && annotation.get("text") != null) {
                if (sb.length() > 0) {
                    sb.append("\n\n");
                }
                sb.append(annotation.get("text"));
            }
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> firstResponse(Map<String, Object> response) {
        if (response == null) {
            return null;
        }
        List<Map<String, Object>> responses = (List<Map<String, Object>>) response.get("responses");
        return responses == null || responses.isEmpty() ? null : responses.get(0);
    }

    @SuppressWarnings("unchecked")
    private static void checkError(Map<String, Object> node) {
        Map<String, Object> error = (Map<String, Object>) node.get("error");
        if (error != null && error.get("message") != null) {
            throw new IllegalStateException("Vision error: " + error.get("message"));
        }
    }
}
