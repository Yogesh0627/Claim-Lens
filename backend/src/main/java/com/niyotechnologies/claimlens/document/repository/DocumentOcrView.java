package com.niyotechnologies.claimlens.document.repository;

/**
 * Minimal projection of a document for the OCR worker: enough to fetch the bytes and label the result.
 * Loaded via a native query that bypasses @TenantId (the worker knows the tenant from the job).
 */
public interface DocumentOcrView {

    Long getId();

    String getStorageKey();

    String getFileName();

    String getContentType();

    String getDocumentType();
}
