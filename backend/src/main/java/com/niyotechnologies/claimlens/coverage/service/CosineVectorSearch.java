package com.niyotechnologies.claimlens.coverage.service;

import com.niyotechnologies.claimlens.coverage.entity.PolicyChunk;
import com.niyotechnologies.claimlens.coverage.repository.PolicyChunkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Default retrieval: load the version's chunks (tenant-scoped by @TenantId) and rank by in-Java cosine
 * over the JSON-text embeddings. No pgvector needed — works on any Postgres. Fine at V1 scale
 * (hundreds of chunks per version). Active unless {@code claimlens.ai.pgvector.enabled=true}.
 */
@Component
@ConditionalOnProperty(name = "claimlens.ai.pgvector.enabled", havingValue = "false", matchIfMissing = true)
@RequiredArgsConstructor
public class CosineVectorSearch implements CoverageVectorSearch {

    @Autowired
    private final PolicyChunkRepository policyChunkRepository;

    @Override
    public void index(PolicyChunk chunk, float[] embedding) {
        // No-op: the embedding already lives in the chunk's JSON-text column, which search reads.
    }

    @Override
    public List<ChunkHit> search(Long versionId, float[] queryVector, int k) {
        return policyChunkRepository.findAllByInsuranceProductVersionId(versionId).stream()
                .map(c -> new ChunkHit(c.getId(), c.getChunkIndex(), c.getChunkText(),
                        Embeddings.cosine(queryVector, Embeddings.deserialize(c.getEmbedding()))))
                .sorted(Comparator.comparingDouble(ChunkHit::score).reversed())
                .limit(k)
                .toList();
    }
}
