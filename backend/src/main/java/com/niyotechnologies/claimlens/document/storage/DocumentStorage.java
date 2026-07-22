package com.niyotechnologies.claimlens.document.storage;

import java.util.Optional;

/**
 * Object-storage seam. The dev implementation writes to the local filesystem; production would be an
 * S3/R2-backed implementation behind the same interface. The DB only ever holds the returned key.
 */
public interface DocumentStorage {

    /** Stores the bytes and returns an opaque storage key (the pointer persisted on the document). */
    String store(byte[] content, String originalFileName);

    byte[] retrieve(String storageKey);

    /**
     * A directly-fetchable public URL for the key, if this backend exposes one (e.g. an R2 public
     * bucket). Used to show image thumbnails inline in HTML emails. Empty for local filesystem storage
     * — callers must degrade gracefully (the PDF still embeds the bytes regardless).
     */
    default Optional<String> publicUrl(String storageKey) {
        return Optional.empty();
    }
}
