package com.niyotechnologies.claimlens.config.database;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables Spring Data JPA auditing, wired to SpringSecurityAuditorAware for @CreatedBy/@LastModifiedBy.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {
}
