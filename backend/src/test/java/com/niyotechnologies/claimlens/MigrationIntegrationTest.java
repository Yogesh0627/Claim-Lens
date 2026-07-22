package com.niyotechnologies.claimlens;

import com.niyotechnologies.claimlens.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Safety net: if the context loads, Flyway applied every migration on a real Postgres AND
 * Hibernate `validate` matched all entities to the schema. This is exactly the check that
 * would have caught the single-underscore V4 bug on the first run.
 */
class MigrationIntegrationTest extends AbstractIntegrationTest {

    @Test
    void migrationsApplyAndSchemaValidates() {
        // Reaching this point means startup (Flyway migrate + Hibernate validate) succeeded.
    }
}
