package com.niyotechnologies.claimlens.processing.analysis;

/**
 * Seam to the analysis service. {@link NoOpAnalysisClient} is the default (tests, un-wired envs);
 * {@code HttpAnalysisClient} calls the real service when {@code claimlens.analysis.enabled=true}.
 * Implementations must never throw — a failure returns {@link AnalysisSignals#none()} so the pipeline
 * still settles.
 */
public interface AnalysisClient {

    AnalysisSignals analyze(byte[] content, String fileName, String contentType);
}
