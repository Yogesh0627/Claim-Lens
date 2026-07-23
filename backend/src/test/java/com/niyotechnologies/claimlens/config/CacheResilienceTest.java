package com.niyotechnologies.claimlens.config;

import com.niyotechnologies.claimlens.config.cache.CacheResilienceConfig;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.interceptor.CacheErrorHandler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * The cache is an optimization, never a dependency. Permission resolution is cached and runs inside
 * the security filter chain on every authenticated request — if a Redis outage propagated out of
 * {@code @Cacheable}, every request would 500 and the health check would fail (which is exactly how
 * the first Render deploy died). This pins the contract: cache errors are logged and swallowed, so
 * the call falls through to its real source.
 */
class CacheResilienceTest {

    private final CacheErrorHandler handler = new CacheResilienceConfig().errorHandler();
    private final Cache cache = new ConcurrentMapCache("rolePermissions");
    private final RuntimeException failure = new RuntimeException("Unable to connect to Redis");

    @Test
    void cacheFailuresAreSwallowedSoTheAppKeepsServing() {
        assertDoesNotThrow(() -> handler.handleCacheGetError(failure, cache, 1L), "get");
        assertDoesNotThrow(() -> handler.handleCachePutError(failure, cache, 1L, "value"), "put");
        assertDoesNotThrow(() -> handler.handleCacheEvictError(failure, cache, 1L), "evict");
        assertDoesNotThrow(() -> handler.handleCacheClearError(failure, cache), "clear");
    }
}
