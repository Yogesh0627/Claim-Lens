package com.niyotechnologies.claimlens.security.ratelimit;

import java.time.Duration;

/**
 * Fixed-window request counter used to throttle abuse-prone and cost-bearing endpoints.
 *
 * <p>Two implementations, chosen by profile — the same swappable-client shape used for storage, OCR,
 * email and the cache: {@link InMemoryRateLimiter} in dev/test (per-instance) and
 * {@link RedisRateLimiter} in prod (shared across instances, so the limit is real behind a load
 * balancer rather than per-pod).
 *
 * <p><b>Implementations must fail OPEN.</b> If the backing store is unreachable, allow the request.
 * A rate limiter exists to blunt abuse; letting it lock every user out during an infrastructure
 * blip trades a small risk for a total outage. (Same principle as the cache — see
 * {@code CacheResilienceConfig}.)
 */
public interface RateLimiter {

    /**
     * Counts one hit against {@code key} and reports whether it is within the allowance.
     *
     * @param key    the bucket — caller identity plus the endpoint group
     * @param limit  hits permitted per window
     * @param window length of the fixed window
     * @return {@code true} if the caller may proceed, {@code false} if it exceeded the limit
     */
    boolean tryAcquire(String key, int limit, Duration window);
}
