package com.niyotechnologies.claimlens.config.cache;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

/**
 * Cache configuration.
 *
 * <p>Two backends, chosen by profile — service code never names either (it just uses
 * {@code @Cacheable}):
 * <ul>
 *   <li><b>local dev / default</b> — Caffeine, in-process (configured declaratively in
 *       application.properties). Upstash lives in the cloud, so a local dev process hitting
 *       it over the network would be slower than the single indexed query it is meant to save.</li>
 *   <li><b>prod</b> — Redis (Upstash), where the app sits next to the cache. This class only
 *       customises that backend.</li>
 * </ul>
 *
 * <p><b>The key prefix is not cosmetic.</b> The Upstash database is shared with another app
 * (EcoExpress, keys prefixed {@code ecoexpress:}) — only one Redis account is in play. Every key
 * ClaimLens writes is prefixed {@code claimlens:} so the apps cannot collide in the same keyspace.
 * (This does not protect against a {@code FLUSHDB} from either side — it namespaces, it does not
 * isolate.)
 */
@Configuration
@Profile("prod")
public class CacheConfig {

    private static final String KEY_PREFIX = "claimlens:";

    @Bean
    public RedisCacheConfiguration redisCacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .prefixCacheNameWith(KEY_PREFIX)
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(cacheObjectMapper())));
    }

    /**
     * JSON, not JDK serialization: cached values stay readable in the Upstash console and survive a
     * class moving between packages.
     *
     * <p>Polymorphic typing is restricted to our own package. Unrestricted default typing on a
     * deserializer is a remote-code-execution vector if anything untrusted ever lands in the cache —
     * and this database is shared with another app.
     */
    private ObjectMapper cacheObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType("com.niyotechnologies.claimlens.")
                        .allowIfBaseType("java.util.")
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        return mapper;
    }
}
