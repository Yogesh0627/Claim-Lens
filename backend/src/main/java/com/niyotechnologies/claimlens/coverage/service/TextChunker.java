package com.niyotechnologies.claimlens.coverage.service;

import java.util.ArrayList;
import java.util.List;

/** Splits policy wording into overlapping chunks on paragraph/sentence boundaries. Overlap preserves
 * context that would otherwise be cut mid-clause. */
final class TextChunker {

    private static final int TARGET_CHARS = 900;
    private static final int OVERLAP_CHARS = 150;

    private TextChunker() {
    }

    static List<String> chunk(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }
        // Normalise whitespace but keep paragraph breaks as soft boundaries.
        String[] paragraphs = text.replace("\r\n", "\n").split("\n\\s*\n");

        StringBuilder current = new StringBuilder();
        for (String paragraph : paragraphs) {
            String p = paragraph.strip();
            if (p.isEmpty()) {
                continue;
            }
            if (current.length() + p.length() + 2 > TARGET_CHARS && current.length() > 0) {
                chunks.add(current.toString().strip());
                current = new StringBuilder(tail(current.toString()));
            }
            if (p.length() > TARGET_CHARS) {
                // A single huge paragraph — hard-split it.
                for (int i = 0; i < p.length(); i += TARGET_CHARS) {
                    chunks.add(p.substring(i, Math.min(p.length(), i + TARGET_CHARS)).strip());
                }
            } else {
                current.append(p).append("\n\n");
            }
        }
        if (current.length() > 0 && !current.toString().isBlank()) {
            chunks.add(current.toString().strip());
        }
        return chunks;
    }

    private static String tail(String text) {
        String t = text.strip();
        return t.length() <= OVERLAP_CHARS ? t + "\n\n" : t.substring(t.length() - OVERLAP_CHARS) + "\n\n";
    }
}
