package com.niyotechnologies.claimlens.security.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Distributed fixed-window limiter backed by Redis (Upstash in production).
 *
 * <p>The counter lives in Redis rather than in each instance's memory, so the limit holds across
 * every instance behind the load balancer. One {@code INCR} per request; the TTL is set only on the
 * first hit of a window, and {@code INCR} is atomic, so concurrent requests can't lose a count.
 *
 * <p>Keys are prefixed {@code claimlens:rl:} — the Upstash database is shared with another app, so
 * namespacing is required (same reason as the cache key prefix in {@code CacheConfig}).
 *
 * <p><b>Fails open.</b> If Redis is unreachable the request is allowed and a warning is logged:
 * a throttle outage must never become a login outage.
 */
@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class RedisRateLimiter implements RateLimiter {

    private static final String KEY_PREFIX = "claimlens:rl:";

    @Autowired
    private final StringRedisTemplate redis;

    @Override
    public boolean tryAcquire(String key, int limit, Duration window) {
        String redisKey = KEY_PREFIX + key;
        try {
            Long count = redis.opsForValue().increment(redisKey);
            if (count == null) {
                return true;
            }
            // First hit of this window — start its expiry. Anything older has already expired, so a
            // fresh counter always gets a fresh window.
            if (count == 1L) {
                redis.expire(redisKey, window);
            }
            return count <= limit;
        } catch (Exception e) {
            log.warn("Rate limiter unavailable ({}) — allowing the request", e.getMessage());
            return true;
        }
    }
}
