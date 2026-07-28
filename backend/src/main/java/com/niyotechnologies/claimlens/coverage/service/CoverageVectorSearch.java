package com.niyotechnologies.claimlens.coverage.service;

import com.niyotechnologies.claimlens.coverage.entity.PolicyChunk;

import java.util.List;

/**
 * The vector store behind RAG retrieval — swappable like the OCR/AI clients. The default
 * {@link CosineVectorSearch} keeps embeddings as JSON text and ranks with in-Java cosine (works on any
 * Postgres, no extension). {@link PgVectorSearch} (when {@code claimlens.ai.pgvector.enabled=true},
 * i.e. on Neon) offloads similarity to a pgvector {@code vector(768)} column with an HNSW index.
 * Retrieval is always tenant-scoped and version-scoped (D7).
 */
public interface CoverageVectorSearch {

    /** Populate any secondary vector index for a just-persisted chunk. No-op for the cosine impl. */
    void index(PolicyChunk chunk, float[] embedding);

    /** Top-K most similar chunks for a product version, scoped to the current tenant. */
    List<ChunkHit> search(Long versionId, float[] queryVector, int k);

    record ChunkHit(Long chunkId, Integer chunkIndex, String chunkText, double score) {
    }
}
