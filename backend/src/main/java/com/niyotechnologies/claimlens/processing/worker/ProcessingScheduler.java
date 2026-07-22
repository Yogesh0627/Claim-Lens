package com.niyotechnologies.claimlens.processing.worker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.function.BooleanSupplier;

/**
 * Drives the processing pipeline in a running app: on each tick it drains the OCR, analysis and fraud
 * queues by calling each worker's {@code pollOnce()} until there is nothing left (capped per tick).
 * Integration tests drive the same {@code pollOnce()} methods synchronously instead, so this is
 * disabled under the test profile ({@code claimlens.processing.scheduler.enabled=false}) to avoid a
 * background poller racing the test's explicit pipeline steps.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "claimlens.processing.scheduler.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class ProcessingScheduler {

    private static final int MAX_PER_TICK = 50;

    @Autowired
    private final OcrWorker ocrWorker;
    @Autowired
    private final AnalysisWorker analysisWorker;
    @Autowired
    private final FraudWorker fraudWorker;

    @Scheduled(fixedDelayString = "${claimlens.processing.scheduler.interval-ms:3000}")
    public void tick() {
        drain(ocrWorker::pollOnce);
        drain(analysisWorker::pollOnce);
        drain(fraudWorker::pollOnce);
    }

    /** Poll until the queue is empty or the per-tick cap is hit (so one tick can't run forever). */
    private void drain(BooleanSupplier poll) {
        int processed = 0;
        try {
            while (processed < MAX_PER_TICK && poll.getAsBoolean()) {
                processed++;
            }
        } catch (Exception e) {
            log.warn("Processing tick error: {}", e.getMessage());
        }
    }
}
