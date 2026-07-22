package com.niyotechnologies.claimlens.processing.analysis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.concurrent.CompletableFuture;

/**
 * Cold-start mitigation for a spun-down analysis-service (e.g. Render free tier). The moment the
 * backend is ready, it fires a fire-and-forget health ping so the analysis-service starts waking
 * <em>in parallel</em> with the user's next actions — instead of only cold-starting later, when the
 * first claim actually needs it. Best-effort: a failed ping (service still waking) is just logged.
 * Active only when {@code claimlens.analysis.enabled=true}.
 */
@Component
@ConditionalOnProperty(name = "claimlens.analysis.enabled", havingValue = "true")
@Slf4j
public class AnalysisWarmup {

    private final RestClient restClient;

    public AnalysisWarmup(
            @Value("${claimlens.analysis.service-url:http://localhost:8001}") String serviceUrl) {
        this.restClient = RestClient.builder().baseUrl(serviceUrl).build();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        // Off the startup thread — waking a cold service can take tens of seconds.
        CompletableFuture.runAsync(() -> {
            try {
                restClient.get().uri("/health").retrieve().toBodilessEntity();
                log.info("Analysis-service warm-up ping sent");
            } catch (Exception e) {
                log.info("Analysis-service warm-up ping failed (it may be waking): {}", e.getMessage());
            }
        });
    }
}
