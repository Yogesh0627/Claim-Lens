package com.niyotechnologies.claimlens.coverage.service;

import com.niyotechnologies.claimlens.coverage.entity.PolicyChunk;
import com.niyotechnologies.claimlens.tenancy.context.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * pgvector-backed retrieval (Neon / prod, {@code claimlens.ai.pgvector.enabled=true}). Similarity is
 * computed in the database against a {@code vector(768)} column with an HNSW index, using the cosine
 * distance operator {@code <=>} (score = 1 − distance). Native SQL bypasses Hibernate's @TenantId, so
 * the tenant filter is applied EXPLICITLY here (the D7 leak guard) alongside the version scope.
 */
@Component
@ConditionalOnProperty(name = "claimlens.ai.pgvector.enabled", havingValue = "true")
@Slf4j
public class PgVectorSearch implements CoverageVectorSearch {

    private final JdbcTemplate jdbc;

    public PgVectorSearch(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void index(PolicyChunk chunk, float[] embedding) {
        try {
            jdbc.update("UPDATE policy_chunk SET embedding_vec = CAST(? AS vector) WHERE id = ?",
                    Embeddings.serialize(embedding), chunk.getId());
        } catch (Exception e) {
            // Best-effort: the JSON-text embedding is still saved; only the ANN index misses this row.
            log.warn("pgvector index failed for chunk {}: {}", chunk.getId(), e.getMessage());
        }
    }

    @Override
    public List<ChunkHit> search(Long versionId, float[] queryVector, int k) {
        String qvec = Embeddings.serialize(queryVector);
        Long tenantId = TenantContext.getTenantId(); // explicit tenant guard (native bypasses @TenantId)
        try {
            return jdbc.query(
                    """
                    SELECT id, chunk_index, chunk_text,
                           1 - (embedding_vec <=> CAST(? AS vector)) AS score
                    FROM policy_chunk
                    WHERE tenant_id = ? AND insurance_product_version_id = ?
                      AND embedding_vec IS NOT NULL
                    ORDER BY embedding_vec <=> CAST(? AS vector)
                    LIMIT ?
                    """,
                    (rs, n) -> new ChunkHit(rs.getLong("id"), rs.getInt("chunk_index"),
                            rs.getString("chunk_text"), rs.getDouble("score")),
                    qvec, tenantId, versionId, qvec, k);
        } catch (Exception e) {
            // Degrade gracefully rather than 500 the whole ask; the chat client then reports it can't
            // find the answer in the wording. Logged so a misconfigured index is visible.
            log.warn("pgvector search failed for version {}: {}", versionId, e.getMessage());
            return List.of();
        }
    }
}
