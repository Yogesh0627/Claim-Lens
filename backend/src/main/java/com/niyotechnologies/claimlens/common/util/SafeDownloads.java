package com.niyotechnologies.claimlens.common.util;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Set;

/**
 * Builds a download response for a stored document without trusting the uploader's declared
 * Content-Type.
 *
 * <p>The stored content type is client-supplied at upload time and is NOT re-derived from bytes, so
 * a document can claim to be {@code text/html} (or {@code image/svg+xml}) while carrying a script.
 * If we echoed that type back with {@code Content-Disposition: inline}, the browser would render it
 * on the app's own origin — a stored-XSS that hands the viewer's localStorage tokens to an attacker.
 * ({@code nosniff} does not help: nothing is being sniffed; the declared type genuinely is HTML.)
 *
 * <p>Rule: a small allowlist of types that are safe to render in-tab (real claim evidence — photos
 * and PDFs) is served {@code inline} with that type; <em>everything else</em> is forced to download
 * as {@code application/octet-stream} with {@code attachment}, so it can never execute in the origin.
 * A stored type that does not even parse falls into the same safe branch (this also removes a
 * persistent 500: {@code MediaType.parseMediaType} on a garbage stored value used to throw on every
 * download).
 */
public final class SafeDownloads {

    /** Types a browser may render in-tab without script execution risk. SVG is deliberately excluded. */
    private static final Set<String> INLINE_SAFE = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "application/pdf");

    private SafeDownloads() {
    }

    public static ResponseEntity<byte[]> of(byte[] content, String fileName, String storedContentType) {
        MediaType parsed = parseOrNull(storedContentType);
        boolean inlineSafe = parsed != null
                && INLINE_SAFE.contains(parsed.getType() + "/" + parsed.getSubtype());

        MediaType responseType = inlineSafe ? parsed : MediaType.APPLICATION_OCTET_STREAM;
        ContentDisposition disposition = (inlineSafe
                ? ContentDisposition.inline()
                : ContentDisposition.attachment())
                .filename(fileName != null ? fileName : "document")
                .build();

        return ResponseEntity.ok()
                .contentType(responseType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(content);
    }

    private static MediaType parseOrNull(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return null;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
