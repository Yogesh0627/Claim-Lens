package com.niyotechnologies.claimlens.security.ratelimit;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Tuning for the rate limiter, one rule per endpoint group.
 *
 * <p>The groups exist for different reasons, so they get different allowances:
 * <ul>
 *   <li><b>auth</b> — abuse control. Unauthenticated, so keyed per client IP. Generous enough for a
 *       person mistyping a password, tight enough to make credential stuffing pointless.</li>
 *   <li><b>ai</b> — <i>cost</i> control. Every coverage question is a paid Gemini call, so an
 *       authenticated user (or a leaked token) could otherwise burn the API budget in minutes.</li>
 *   <li><b>upload</b> — cost control too: each document triggers object storage plus Vision OCR,
 *       which bills per page.</li>
 * </ul>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "claimlens.security.rate-limit")
public class RateLimitProperties {

    /** Master switch. Off in tests so suites aren't throttled. */
    private boolean enabled = true;

    /** Sign-in / refresh — keyed per client IP (there is no principal yet). */
    private Rule auth = new Rule(20, Duration.ofMinutes(1));

    /** Coverage Q&A and knowledge ingest — keyed per user; each call costs Gemini credits. */
    private Rule ai = new Rule(30, Duration.ofMinutes(1));

    /** Document uploads — keyed per user; each costs storage + OCR. */
    private Rule upload = new Rule(60, Duration.ofMinutes(1));

    @Getter
    @Setter
    public static class Rule {
        private int limit;
        private Duration window;

        public Rule() {
        }

        public Rule(int limit, Duration window) {
            this.limit = limit;
            this.window = window;
        }
    }
}
