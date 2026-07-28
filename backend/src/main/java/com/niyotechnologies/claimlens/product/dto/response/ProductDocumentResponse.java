package com.niyotechnologies.claimlens.product.dto.response;

import java.time.Instant;

/** Metadata for a product-version document (e.g. a policy-wording PDF). Bytes are downloaded separately. */
public record ProductDocumentResponse(
        Long id,
        String documentType,
        String fileName,
        String contentType,
        Long sizeBytes,
        Instant createdAt,
        // Set on the upload response when the file's text was auto-extracted and ingested into the AI
        // knowledge base — the number of chunks indexed. Null when not applicable (non-PDF, no text,
        // or on a plain list where it isn't tracked per row).
        Integer indexedSections
) {
    /** Convenience for the common metadata-only case (list rows), where auto-ingest isn't reported. */
    public ProductDocumentResponse(Long id, String documentType, String fileName, String contentType,
                                   Long sizeBytes, Instant createdAt) {
        this(id, documentType, fileName, contentType, sizeBytes, createdAt, null);
    }
}
