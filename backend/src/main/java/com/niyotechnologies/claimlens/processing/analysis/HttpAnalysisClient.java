package com.niyotechnologies.claimlens.processing.analysis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * Calls the Python analysis service over HTTP. Active only when {@code claimlens.analysis.enabled=true}.
 * Never throws: any failure returns {@link AnalysisSignals#none()} so the pipeline settles regardless.
 */
@Component
@ConditionalOnProperty(name = "claimlens.analysis.enabled", havingValue = "true")
@Slf4j
public class HttpAnalysisClient implements AnalysisClient {

    private final RestClient restClient;
    private final String sharedSecret;

    public HttpAnalysisClient(
            @Value("${claimlens.analysis.service-url:http://localhost:8001}") String serviceUrl,
            @Value("${claimlens.analysis.shared-secret:}") String sharedSecret) {
        // SimpleClientHttpRequestFactory buffers the request body (so Content-Length is set and the
        // multipart bytes are actually written). The default JDK-HttpClient factory sends an EMPTY
        // body for Spring multipart -> the server sees no 'file' part (422). This is the fix.
        this.restClient = RestClient.builder()
                .baseUrl(serviceUrl)
                .requestFactory(new SimpleClientHttpRequestFactory())
                .build();
        this.sharedSecret = sharedSecret;
    }

    @Override
    public AnalysisSignals analyze(byte[] content, String fileName, String contentType) {
        try {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", new ByteArrayResource(content) {
                @Override
                public String getFilename() {
                    return fileName != null ? fileName : "image";
                }
            }).contentType(MediaType.APPLICATION_OCTET_STREAM);

            // Do NOT set contentType here — let the multipart converter set
            // multipart/form-data WITH its generated boundary (an explicit contentType strips it,
            // and the server then can't split the parts -> 422 "file field required").
            AnalyzeResponse response = restClient.post()
                    .uri("/analyze")
                    .headers(h -> {
                        if (StringUtils.hasText(sharedSecret)) {
                            h.add("X-ANALYSIS-TOKEN", sharedSecret);
                        }
                    })
                    .body(builder.build())
                    .retrieve()
                    .body(AnalyzeResponse.class);

            if (response == null || response.hashes() == null) {
                return AnalysisSignals.none();
            }
            return new AnalysisSignals(
                    response.hashes().phash(),
                    response.hashes().dhash(),
                    response.hashes().average(),
                    response.exif() != null ? response.exif().state() : "UNKNOWN",
                    response.synthetic() != null && response.synthetic().signal(),
                    response.synthetic() != null ? response.synthetic().score() : null,
                    false);
        } catch (Exception ex) {
            log.warn("Analysis service call failed for '{}': {}", fileName, ex.getMessage());
            return AnalysisSignals.none();
        }
    }

    private record AnalyzeResponse(Hashes hashes, Exif exif, Synthetic synthetic) {
    }

    private record Hashes(String average, String dhash, String phash) {
    }

    private record Exif(String state) {
    }

    private record Synthetic(boolean signal, Double score) {
    }
}
