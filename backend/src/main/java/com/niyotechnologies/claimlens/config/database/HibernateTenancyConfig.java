package com.niyotechnologies.claimlens.config.database;

import com.niyotechnologies.claimlens.tenancy.context.ClaimLensTenantIdentifierResolver;
import lombok.RequiredArgsConstructor;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Registers the tenant resolver with Hibernate. Spring Boot does not auto-detect a
 * CurrentTenantIdentifierResolver bean for discriminator (@TenantId) multi-tenancy,
 * so it must be wired in explicitly.
 */
@Configuration
@RequiredArgsConstructor
public class HibernateTenancyConfig implements HibernatePropertiesCustomizer {

    @Autowired
    private final ClaimLensTenantIdentifierResolver tenantIdentifierResolver;

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(
                AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER,
                tenantIdentifierResolver
        );
    }
}
