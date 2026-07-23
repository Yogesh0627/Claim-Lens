package com.niyotechnologies.claimlens.security.ratelimit;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Per-instance fixed-window limiter for dev and tests — no infrastructure required.
 *
 * <p>Deliberately NOT used in production: with more than one instance behind a load balancer each
 * would keep its own counter, so the effective limit would multiply by the instance count. That's
 * what {@link RedisRateLimiter} solves.
 */
@Component
@Profile("!prod")
public class InMemoryRateLimiter implements RateLimiter {

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    public boolean tryAcquire(String key, int limit, Duration window) {
        Instant now = Instant.now();
        Window current = windows.compute(key, (k, existing) ->
                (existing == null || now.isAfter(existing.expiresAt))
                        ? new Window(now.plus(window))
                        : existing);
        // Opportunistic cleanup so a long-running dev server doesn't accumulate dead keys.
        if (windows.size() > 10_000) {
            windows.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt));
        }
        return current.count.incrementAndGet() <= limit;
    }

    private static final class Window {
        private final Instant expiresAt;
        private final AtomicInteger count = new AtomicInteger();

        private Window(Instant expiresAt) {
            this.expiresAt = expiresAt;
        }
    }
}
