package com.niyotechnologies.claimlens.config.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Makes the cache a strict optimization, never a dependency.
 *
 * <p>By default Spring lets a cache failure propagate out of {@code @Cacheable}. That is the wrong
 * trade here: permission resolution is cached and runs inside the security filter chain on *every*
 * authenticated request, so an unreachable Redis would turn a cache outage into a total outage —
 * every request 500s. (Exactly what happened on the first deploy.)
 *
 * <p>With this handler a cache error is logged and swallowed, so the call falls through to its real
 * source (one indexed query). The app runs correctly — just without the cache — until Redis returns.
 */
@Slf4j
@Configuration
public class CacheResilienceConfig implements CachingConfigurer {

    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {

            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                warn("get", cache, exception);
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                warn("put", cache, exception);
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                warn("evict", cache, exception);
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                warn("clear", cache, exception);
            }

            private void warn(String op, Cache cache, RuntimeException e) {
                log.warn("Cache {} failed on '{}' — ignoring and using the source. Cause: {}",
                        op, cache.getName(), e.getMessage());
            }
        };
    }
}
