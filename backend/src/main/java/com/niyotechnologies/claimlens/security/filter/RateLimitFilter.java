package com.niyotechnologies.claimlens.security.filter;

import com.niyotechnologies.claimlens.security.model.ClaimLensPrincipal;
import com.niyotechnologies.claimlens.security.ratelimit.RateLimitProperties;
import com.niyotechnologies.claimlens.security.ratelimit.RateLimitProperties.Rule;
import com.niyotechnologies.claimlens.security.ratelimit.RateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Throttles the two kinds of endpoint that need it, for two different reasons:
 *
 * <ul>
 *   <li><b>Abuse</b> — {@code /auth/**} is the only unauthenticated surface, so it is the only
 *       brute-forceable one. Keyed per client IP, since there is no principal yet.</li>
 *   <li><b>Cost</b> — coverage Q&amp;A / knowledge ingest each make a paid Gemini call, and every
 *       document upload triggers object storage plus per-page Vision OCR. Authenticated, but a
 *       compromised token or a runaway client could burn the API budget in minutes. Keyed per user,
 *       so one tenant's abuse cannot throttle everyone else.</li>
 * </ul>
 *
 * <p>Runs <b>after</b> {@code JwtAuthenticationFilter} so the principal is available for per-user
 * keying — while still sitting ahead of the controllers, so a throttled login is rejected before any
 * password hashing or database work. Requests that match no rule (ordinary reads and writes) are
 * never throttled.
 *
 * <p>Not a {@code @Component}: it is constructed in {@code SecurityConfig}. Registering it as a bean
 * would make Boot ALSO auto-register it on the raw servlet chain, where it would run a second time
 * and double-count every request — the same trap documented on {@link JwtAuthenticationFilter}.
 */
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter rateLimiter;
    private final RateLimitProperties properties;
    private final String basePath;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!properties.isEnabled()) {
            chain.doFilter(request, response);
            return;
        }

        String uri = request.getRequestURI();
        Bucket bucket = bucketFor(uri, request.getMethod());
        if (bucket == null) {
            chain.doFilter(request, response);
            return;
        }

        String key = bucket.name + ":" + callerId(request, bucket.perUser) + ":" + bucket.scope;
        if (rateLimiter.tryAcquire(key, bucket.rule.getLimit(), bucket.rule.getWindow())) {
            chain.doFilter(request, response);
            return;
        }

        long retryAfter = Math.max(1, bucket.rule.getWindow().toSeconds());
        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(retryAfter));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"success\":false,\"code\":\"RATE_LIMIT_EXCEEDED\","
                        + "\"message\":\"Too many requests. Please try again in "
                        + retryAfter + " seconds.\"}");
    }

    /** Which rule (if any) applies. Null means "not throttled". */
    private Bucket bucketFor(String uri, String method) {
        if (uri.startsWith(basePath + "/auth")) {
            // Per-endpoint scope so a login flood can't also lock out token refresh.
            return new Bucket("auth", properties.getAuth(), false, uri);
        }
        if (uri.startsWith(basePath + "/coverage") || uri.contains("/knowledge")) {
            return new Bucket("ai", properties.getAi(), true, "ai");
        }
        // Uploads only — reading or listing documents is cheap.
        if (uri.contains("/documents") && HttpMethod.POST.matches(method)) {
            return new Bucket("upload", properties.getUpload(), true, "upload");
        }
        return null;
    }

    /** Authenticated caller → user id; otherwise the client IP. */
    private String callerId(HttpServletRequest request, boolean perUser) {
        if (perUser) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof ClaimLensPrincipal principal) {
                return "u" + principal.userId();
            }
        }
        return "ip" + clientIp(request);
    }

    /**
     * Behind Render's proxy the socket address is the load balancer, so every caller would share one
     * bucket. Prefer the first hop in X-Forwarded-For, which is the real client.
     */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String remote = request.getRemoteAddr();
        return remote == null ? "unknown" : remote;
    }

    private record Bucket(String name, Rule rule, boolean perUser, String scope) {
    }
}
