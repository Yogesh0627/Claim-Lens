package com.niyotechnologies.claimlens.processing.support;

/**
 * String status values for claim_processing_state (kept as strings so the exactly-once fraud gate can
 * be a plain conditional JPQL UPDATE). A stage is "settled" (COMPLETE) when all its jobs are terminal.
 */
public final class ProcessingStatus {

    public static final String NOT_STARTED = "NOT_STARTED";
    public static final String PENDING = "PENDING";
    public static final String PROCESSING = "PROCESSING";
    public static final String COMPLETE = "COMPLETE";
    public static final String FAILED = "FAILED";
    public static final String QUEUED = "QUEUED";

    private ProcessingStatus() {
    }
}
