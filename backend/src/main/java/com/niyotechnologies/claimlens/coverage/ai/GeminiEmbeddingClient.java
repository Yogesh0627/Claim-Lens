package com.niyotechnologies.claimlens.coverage.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Google Gemini embeddings (gemini-embedding-001, 768-dim). Active only when
 * {@code claimlens.ai.enabled=true} with a GEMINI_API_KEY. Semantic — the real retrieval quality. If
 * the provider is unavailable (bad key, quota/billing 429, network), it degrades gracefully to an
 * offline feature-hash embedding at the SAME dimension rather than failing the request.
 */
@Component
@ConditionalOnProperty(name = "claimlens.ai.enabled", havingValue = "true")
@Slf4j
public class GeminiEmbeddingClient implements EmbeddingClient {

    private final RestClient restClient;
    private final String apiKey;
    private final String embeddingModel;
    private final int dimension;

    public GeminiEmbeddingClient(
            @Value("${claimlens.ai.gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl,
            @Value("${claimlens.ai.gemini.api-key:}") String apiKey,
            @Value("${claimlens.ai.gemini.embedding-model:gemini-embedding-001}") String embeddingModel,
            @Value("${claimlens.ai.gemini.embedding-dimension:768}") int dimension) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.embeddingModel = embeddingModel;
        this.dimension = dimension;
    }

    @Override
    public String model() {
        return embeddingModel;
    }

    @Override
    @SuppressWarnings("unchecked")
    public float[] embed(String text) {
        Map<String, Object> body = Map.of(
                "model", "models/" + embeddingModel,
                "content", Map.of("parts", List.of(Map.of("text", text))),
                "outputDimensionality", dimension);
        try {
            Map<String, Object> response = restClient.post()
                    .uri(uri -> uri.path("/models/{model}:embedContent").queryParam("key", apiKey)
                            .build(embeddingModel))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            Map<String, Object> embedding = response == null ? null : (Map<String, Object>) response.get("embedding");
            List<Number> values = embedding == null ? null : (List<Number>) embedding.get("values");
            if (values == null || values.isEmpty()) {
                log.warn("Gemini returned no embedding — falling back to feature-hash embedding");
                return FeatureHashEmbedding.embed(text, dimension);
            }
            float[] vector = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                vector[i] = values.get(i).floatValue();
            }
            return vector;
        } catch (Exception e) {
            // Bad key, quota/billing (429), network — degrade rather than fail the request.
            log.warn("Gemini embedding failed ({}) — falling back to feature-hash embedding", e.getMessage());
            return FeatureHashEmbedding.embed(text, dimension);
        }
    }
}
