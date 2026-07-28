package com.niyotechnologies.claimlens.coverage.ai;

/**
 * Signed feature-hashing embedding, parameterised by dimension. Deterministic and offline — cosine
 * between two of these reflects shared vocabulary. Used by {@link StubEmbeddingClient} and as the
 * graceful fallback when a real provider (Gemini) is unavailable, at the provider's own dimension so
 * fallback vectors stay compatible with already-stored ones.
 */
final class FeatureHashEmbedding {

    private FeatureHashEmbedding() {
    }

    static float[] embed(String text, int dim) {
        float[] vector = new float[dim];
        if (text == null || text.isBlank()) {
            return vector;
        }
        for (String token : text.toLowerCase().split("[^a-z0-9]+")) {
            if (token.length() < 2) {
                continue;
            }
            int bucket = Math.floorMod(token.hashCode(), dim);
            int sign = Math.floorMod(token.hashCode() * 31 + 7, 2) == 0 ? 1 : -1;
            vector[bucket] += sign;
        }
        double norm = 0;
        for (float v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < dim; i++) {
                vector[i] = (float) (vector[i] / norm);
            }
        }
        return vector;
    }
}
