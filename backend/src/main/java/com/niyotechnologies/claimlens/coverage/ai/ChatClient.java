package com.niyotechnologies.claimlens.coverage.ai;

import java.util.List;

/**
 * Generates a grounded answer from the question and the retrieved policy excerpts. Stub by default
 * (extractive), Gemini when {@code claimlens.ai.enabled=true}.
 */
public interface ChatClient {

    /**
     * The answer text plus the model that ACTUALLY produced it.
     *
     * <p>The provider may degrade internally (Gemini failing over to the extractive fallback), so the
     * configured model from {@link #model()} is not proof of what answered. Persisting and returning
     * this value is what makes a degraded answer distinguishable from a synthesised one — without it,
     * a fallback is indistinguishable from success at every layer above.
     */
    record Answer(String text, String model) {
    }

    /** The configured model — what this client will use when everything works. */
    String model();

    Answer answer(String question, List<String> contexts);
}
