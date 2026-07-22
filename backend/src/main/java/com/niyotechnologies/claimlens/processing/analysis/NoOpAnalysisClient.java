package com.niyotechnologies.claimlens.processing.analysis;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default analysis client: does nothing. Active unless {@code claimlens.analysis.enabled=true}, so
 * tests and un-wired environments run the pipeline exactly as before.
 */
@Component
@ConditionalOnProperty(name = "claimlens.analysis.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpAnalysisClient implements AnalysisClient {

    @Override
    public AnalysisSignals analyze(byte[] content, String fileName, String contentType) {
        return AnalysisSignals.none();
    }
}
