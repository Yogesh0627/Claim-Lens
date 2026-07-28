package com.niyotechnologies.claimlens.coverage.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Default answerer: extractive, no LLM. Returns the most relevant retrieved excerpts verbatim under a
 * clear preamble. Honest and deterministic — good for offline demos and tests. Enable the AI provider
 * for a synthesised natural-language answer.
 */
@Component
@ConditionalOnProperty(name = "claimlens.ai.enabled", havingValue = "false", matchIfMissing = true)
public class StubChatClient implements ChatClient {

    @Override
    public String model() {
        return "stub-extractive";
    }

    @Override
    public Answer answer(String question, List<String> contexts) {
        return new Answer(answerText(question, contexts), model());
    }

    /** Exposed so a degrading provider can reuse the extractive body while reporting its own model. */
    String answerText(String question, List<String> contexts) {
        if (contexts == null || contexts.isEmpty()) {
            return "No policy wording has been ingested for this product version, so I can't answer "
                    + "coverage questions yet.";
        }
        StringBuilder sb = new StringBuilder("Based on the most relevant policy wording:\n\n");
        int shown = Math.min(contexts.size(), 3);
        for (int i = 0; i < shown; i++) {
            sb.append("• ").append(truncate(contexts.get(i))).append("\n\n");
        }
        sb.append("(Extractive answer — enable the AI provider for a synthesised response.)");
        return sb.toString();
    }

    private static String truncate(String text) {
        String clean = text.strip();
        return clean.length() > 400 ? clean.substring(0, 400) + "…" : clean;
    }
}
