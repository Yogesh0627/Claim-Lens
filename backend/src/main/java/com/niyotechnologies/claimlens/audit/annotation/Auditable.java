package com.niyotechnologies.claimlens.audit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service method whose successful invocation should be recorded in the audit trail.
 * The AuditAspect captures the acting user (from the security context), the tenant, the timestamp,
 * and the entity id (the first Long argument, by convention the id).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    String action();

    String entityType();
}
