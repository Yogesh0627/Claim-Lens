package com.niyotechnologies.claimlens.coverage.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Google Gemini chat (gemini-3-flash-preview by default) for grounded coverage answers. Active only
 * when {@code claimlens.ai.enabled=true}. The prompt constrains it to answer strictly from the
 * excerpts. If the provider is unavailable (bad key, quota/billing, network), it degrades gracefully
 * to the extractive answer (the retrieved excerpts) rather than failing the request.
 */
@Component
@ConditionalOnProperty(name = "claimlens.ai.enabled", havingValue = "true")
@Slf4j
public class GeminiChatClient implements ChatClient {

    /** Two attempts total: one retry is enough for a load-shed 503 without stalling the request. */
    private static final int MAX_ATTEMPTS = 2;
    private static final long RETRY_DELAY_MS = 400L;

    // Typed as the concrete stub, not ChatClient, so the extractive body can be reused while this
    // client reports its own (fallback-marked) model.
    private final StubChatClient fallback = new StubChatClient();
    private final RestClient restClient;
    private final String apiKey;
    private final String chatModel;

    public GeminiChatClient(
            @Value("${claimlens.ai.gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl,
            @Value("${claimlens.ai.gemini.api-key:}") String apiKey,
            @Value("${claimlens.ai.gemini.chat-model:gemini-1.5-flash}") String chatModel) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.chatModel = chatModel;
    }

    @Override
    public String model() {
        return chatModel;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Answer answer(String question, List<String> contexts) {
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", buildPrompt(question, contexts))))));
        try {
            Map<String, Object> response = callWithRetry(body);

            List<Map<String, Object>> candidates =
                    response == null ? null : (List<Map<String, Object>>) response.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                return degrade(question, contexts, "no candidates returned");
            }

            // A thinking model (gemini-3-*) can return a candidate with NO content/parts at all — e.g.
            // when the thinking budget consumes the response, or generation stops early. Reaching
            // straight into content.get("parts") NPEs there, which the catch below silently turned
            // into a fallback. Navigate defensively and report the real reason instead.
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts =
                    content == null ? null : (List<Map<String, Object>>) content.get("parts");
            if (parts == null || parts.isEmpty()) {
                Object finishReason = candidates.get(0).get("finishReason");
                return degrade(question, contexts, "no parts (finishReason=" + finishReason + ")");
            }

            // Skip parts that carry no text (thought parts). String.valueOf(null) would otherwise
            // splice the literal "null" into the answer.
            String text = parts.stream()
                    .map(p -> p.get("text"))
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .reduce("", (a, b) -> a + b)
                    .strip();

            if (text.isEmpty()) {
                return degrade(question, contexts, "empty text in all parts");
            }
            return new Answer(text, chatModel);
        } catch (Exception e) {
            // Bad key, quota/billing, network — degrade to the retrieved excerpts rather than failing.
            return degrade(question, contexts, e.getMessage());
        }
    }

    /**
     * Calls Gemini, retrying once on a 5xx only.
     *
     * <p>{@code gemini-*-preview} models shed load with 503 UNAVAILABLE ("experiencing high demand").
     * That is genuinely transient and a short retry recovers it.
     *
     * <p><b>429 is deliberately NOT retried.</b> The free tier caps generate_content at 20 requests
     * per minute, and the 429 body states its own retry delay — measured at ~28s. Retrying after a
     * few hundred ms cannot succeed, and it spends another request against the very quota that is
     * exhausted, making the limit harder to escape. Measured directly: adding a short-delay 429 retry
     * took degraded answers from 1-in-8 to 8-in-12. Fail straight to the extractive fallback instead.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> callWithRetry(Map<String, Object> body) {

        HttpServerErrorException last = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return restClient.post()
                        .uri(uri -> uri.path("/models/{model}:generateContent").queryParam("key", apiKey)
                                .build(chatModel))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(Map.class);

            } catch (HttpServerErrorException e) {
                last = e;

                if (attempt < MAX_ATTEMPTS) {
                    log.warn("Gemini 5xx ({}) — retrying {}/{}",
                            e.getStatusCode(), attempt + 1, MAX_ATTEMPTS);
                    sleep(RETRY_DELAY_MS * attempt);
                }
            }
        }

        throw last;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Falls back to the extractive answer, reporting the FALLBACK's model rather than Gemini's.
     *
     * <p>Previously the caller was told {@code model=gemini-…} even when the stub produced the text,
     * which made a silent degradation indistinguishable from a real synthesised answer — in the API
     * response, in the persisted {@code coverage_answer} row, and in the UI.
     */
    private Answer degrade(String question, List<String> contexts, String reason) {
        log.warn("Gemini chat unavailable ({}) — falling back to extractive answer", reason);
        return new Answer(fallback.answerText(question, contexts), fallback.model() + " (gemini-fallback)");
    }

    private static String buildPrompt(String question, List<String> contexts) {
        String excerpts = IntStream.range(0, contexts.size())
                .mapToObj(i -> "[" + (i + 1) + "] " + contexts.get(i))
                .reduce("", (a, b) -> a + "\n\n" + b);
        return """
                You are a motor-insurance policy assistant. Answer the question using ONLY the policy
                excerpts below. If the excerpts do not cover it, say the policy wording does not address
                this. Be concise and cite the excerpt numbers you used.

                Policy excerpts:%s

                Question: %s
                """.formatted(excerpts, question);
    }
}
