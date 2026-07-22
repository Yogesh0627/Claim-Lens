package com.niyotechnologies.claimlens.document.dto.response;

/** The raw bytes of a stored document plus the metadata needed to serve it back to a client. */
public record DocumentContent(
        byte[] content,
        String fileName,
        String contentType
) {
}
