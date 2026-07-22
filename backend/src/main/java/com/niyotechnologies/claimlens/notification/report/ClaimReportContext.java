package com.niyotechnologies.claimlens.notification.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Everything the email + PDF renderers need about a claim, as plain values — no JPA entities, so the
 * report layer never triggers lazy loads or drags claim internals into the notification package.
 * Assembled by the claim service (which has all the repositories) at the moment an email is sent.
 *
 * @param incidentDescription the customer's account of how the incident happened (the claim narrative)
 * @param documents           every document on the claim (RC, DL, FIR, invoices, photos …). The email
 *                            and PDF list them all by type + name; image documents additionally get a
 *                            thumbnail (email links by {@code publicUrl}, PDF embeds their bytes).
 */
public record ClaimReportContext(
        String claimNumber,
        String statusLabel,
        StatusTone statusTone,
        String customerName,
        String policyNumber,
        String vehicleRegistration,
        BigDecimal claimAmount,
        LocalDate incidentDate,
        LocalDate reportedDate,
        String incidentDescription,
        String investigatorName,
        String decidedByName,
        String decisionReason,
        Integer fraudScore,
        String fraudRiskLevel,
        List<DocRef> documents) {

    public ClaimReportContext {
        documents = documents == null ? List.of() : List.copyOf(documents);
    }

    /** Convenience: just the image documents, in order — the ones that get a thumbnail / embed. */
    public List<DocRef> imageDocuments() {
        return documents.stream().filter(DocRef::isImage).toList();
    }

    /**
     * A document on the claim. {@code documentType} is the raw upload type (e.g. {@code RC_BOOK});
     * {@code publicUrl} may be null (local dev); {@code storageKey} always resolves.
     */
    public record DocRef(String fileName, String documentType, String contentType,
                         String publicUrl, String storageKey) {
        public boolean isImage() {
            return contentType != null && contentType.startsWith("image/");
        }
    }

    /** Drives the badge colour without leaking the claim status enum into this package. */
    public enum StatusTone {
        NEUTRAL, INFO, POSITIVE, NEGATIVE
    }
}
