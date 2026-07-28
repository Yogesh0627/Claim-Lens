package com.niyotechnologies.claimlens.coverage.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Enables pgvector on startup when {@code claimlens.ai.pgvector.enabled=true} (Neon / prod). Kept out
 * of Flyway on purpose so the local/test databases — which don't have the pgvector extension — never
 * try to create it. All statements are idempotent, so this is safe to run on every boot. The column
 * is unmapped by JPA (used only via native SQL in {@link PgVectorSearch}), so {@code ddl-auto=validate}
 * is unaffected.
 */
@Component
@ConditionalOnProperty(name = "claimlens.ai.pgvector.enabled", havingValue = "true")
@Slf4j
public class PgVectorInitializer implements ApplicationRunner {

    private static final int EMBEDDING_DIM = 768; // gemini-embedding-001

    private final JdbcTemplate jdbc;

    public PgVectorInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbc.execute("CREATE EXTENSION IF NOT EXISTS vector");
            jdbc.execute("ALTER TABLE policy_chunk ADD COLUMN IF NOT EXISTS embedding_vec vector("
                    + EMBEDDING_DIM + ")");
            // HNSW needs no training data (unlike ivfflat), so it can be created up front on an empty
            // table and it self-maintains as rows arrive.
            jdbc.execute("CREATE INDEX IF NOT EXISTS idx_policy_chunk_hnsw "
                    + "ON policy_chunk USING hnsw (embedding_vec vector_cosine_ops)");
            log.info("pgvector enabled: extension + policy_chunk.embedding_vec + HNSW index ready");
        } catch (Exception e) {
            log.error("pgvector initialization failed — RAG will fall back to empty results until fixed: {}",
                    e.getMessage());
        }
    }
}
