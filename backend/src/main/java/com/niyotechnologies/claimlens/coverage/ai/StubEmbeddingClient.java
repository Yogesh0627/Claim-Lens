package com.niyotechnologies.claimlens.coverage.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default embedding: a signed feature-hashing vectoriser (like a lightweight HashingVectorizer). No
 * API key, deterministic, and — crucially — cosine similarity between two of these vectors reflects
 * shared vocabulary, so retrieval genuinely surfaces the relevant policy chunks offline. Not semantic
 * like a real model, but enough to run and demo RAG end-to-end without a provider.
 */
@Component
@ConditionalOnProperty(name = "claimlens.ai.enabled", havingValue = "false", matchIfMissing = true)
public class StubEmbeddingClient implements EmbeddingClient {

    private static final int DIM = 256;

    @Override
    public String model() {
        return "stub-hash-" + DIM;
    }

    @Override
    public float[] embed(String text) {
        return FeatureHashEmbedding.embed(text, DIM);
    }
}
