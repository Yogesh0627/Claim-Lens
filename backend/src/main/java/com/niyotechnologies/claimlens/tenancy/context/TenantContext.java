package com.niyotechnologies.claimlens.tenancy.context;

/**
 * Holds the current request's tenant id in a ThreadLocal.
 *
 * Deliberately NOT an InheritableThreadLocal: that would leak the tenant across pooled
 * threads and hand @Async children a stale tenant. The value is set by TenantFilter from
 * the authenticated principal and cleared in that filter's finally block.
 */
public final class TenantContext {

    /**
     * Sentinel used when no tenant is bound (startup, permit-all endpoints, background jobs).
     * The Hibernate resolver returns this so a tenant-scoped query matches no real tenant's rows.
     */
    public static final Long SYSTEM_TENANT = -1L;

    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(Long tenantId) {
        CURRENT.set(tenantId);
    }

    /**
     * @throws IllegalStateException if no tenant is bound — never returns null silently.
     */
    public static Long getTenantId() {
        Long tenantId = CURRENT.get();
        if (tenantId == null) {
            throw new IllegalStateException("No tenant bound to the current thread");
        }
        return tenantId;
    }

    public static Long getTenantIdOrNull() {
        return CURRENT.get();
    }

    public static boolean isSet() {
        return CURRENT.get() != null;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
