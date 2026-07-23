package com.niyotechnologies.claimlens.config;

import com.niyotechnologies.claimlens.config.cache.CacheConfig;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trips a cached value through the REAL Redis serializer — no Redis required.
 *
 * <p>This exists because of a bug that reached production: the value serializer was configured with
 * {@code allowIfBaseType}, but cache values are declared as {@code Object}, so the base type is
 * always {@code java.lang.Object} and the rule matched nothing. Writes succeeded and every read was
 * denied, so the cache was a silent 100% miss — invisible, because the error handler swallowed it and
 * the app fell back to the database. Nothing failed; it just quietly did no caching.
 *
 * <p>It was invisible to the suite too: tests run with {@code spring.cache.type=none} and local dev
 * uses Caffeine, so the Redis serializer ran nowhere. This test closes that gap.
 */
class CacheSerializationTest {

    /** The exact serializer the prod cache uses. */
    private SerializationPair<Object> valueSerializer() {
        RedisCacheConfiguration config = new CacheConfig().redisCacheConfiguration();
        return config.getValueSerializationPair();
    }

    @Test
    void aCachedPermissionSetSurvivesTheRoundTrip() {
        // Exactly what PermissionService caches: Set<String> (a HashSet at runtime).
        Set<String> permissions = new HashSet<>(Set.of("CLAIM_READ", "CLAIM_WRITE", "CLAIM_DECIDE"));

        ByteBuffer written = valueSerializer().write(permissions);
        Object read = valueSerializer().read(written);

        assertEquals(permissions, read, "the cached value must deserialize back to an equal Set");
        assertTrue(read instanceof Set<?>, "type must survive, not degrade to a raw list/map");
    }

    @Test
    void anEmptySetAlsoRoundTrips() {
        Set<String> empty = new HashSet<>();
        assertEquals(empty, valueSerializer().read(valueSerializer().write(empty)));
    }

    @Test
    void theKeyPrefixNamespacesThisAppOnASharedRedis() {
        // The Upstash database is shared with another app, so every key must be prefixed.
        String prefixed = new CacheConfig().redisCacheConfiguration()
                .getKeyPrefixFor("rolePermissions");
        assertTrue(prefixed.startsWith("claimlens:"),
                "keys must be namespaced to avoid colliding with the other app, got: " + prefixed);
    }
}
