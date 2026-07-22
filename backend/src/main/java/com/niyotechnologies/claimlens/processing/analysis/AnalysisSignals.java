package com.niyotechnologies.claimlens.processing.analysis;

/**
 * Image fraud signals for one document, from the analysis service. Perceptual hashes drive cross-claim
 * duplicate detection (the backend compares them); EXIF state and the synthetic signal are hints.
 * Signals, never decisions.
 */
public record AnalysisSignals(
        String phash,
        String dhash,
        String averageHash,
        String exifState,        // CONSISTENT | INCONSISTENT | UNKNOWN
        boolean syntheticSignal,
        Double syntheticScore,
        boolean empty
) {

    public static AnalysisSignals none() {
        return new AnalysisSignals(null, null, null, "UNKNOWN", false, null, true);
    }
}
