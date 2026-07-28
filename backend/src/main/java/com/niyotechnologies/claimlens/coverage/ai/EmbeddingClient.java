package com.niyotechnologies.claimlens.coverage.ai;

/**
 * Turns text into a vector. One bean is active at a time (stub by default, Gemini when
 * {@code claimlens.ai.enabled=true}), so the query and the stored chunks are always embedded by the
 * same model — a prerequisite for cosine comparison.
 */
public interface EmbeddingClient {

    String model();

    float[] embed(String text);
}
