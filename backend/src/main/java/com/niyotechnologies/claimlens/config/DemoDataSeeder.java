package com.niyotechnologies.claimlens.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Seeds a self-contained "Demo Insurance" tenant (one user per role + curated claims/policies) so a
 * fresh deployment is instantly explorable. Runs after Flyway, only when {@code claimlens.demo.seed=true},
 * and is idempotent (the SQL guards on the demo tenant already existing). SANDBOX data only.
 */
@Component
@ConditionalOnProperty(name = "claimlens.demo.seed", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DemoDataSeeder implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        try {
            // crypt()/gen_salt() are used to hash the demo password portably (Neon supports pgcrypto).
            jdbc.execute("CREATE EXTENSION IF NOT EXISTS pgcrypto");
            String sql = new ClassPathResource("demo-seed.sql").getContentAsString(StandardCharsets.UTF_8);
            jdbc.execute(sql);
            log.info("Demo data seed executed (idempotent).");
        } catch (Exception e) {
            log.warn("Demo data seed skipped/failed: {}", e.getMessage());
        }
    }
}
