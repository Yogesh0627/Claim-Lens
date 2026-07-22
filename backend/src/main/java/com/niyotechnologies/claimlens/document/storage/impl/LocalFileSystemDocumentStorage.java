package com.niyotechnologies.claimlens.document.storage.impl;

import com.niyotechnologies.claimlens.common.exception.BusinessException;
import com.niyotechnologies.claimlens.document.storage.DocumentStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Dev/default storage: writes bytes under a local directory. Active unless
 * {@code claimlens.storage.provider=s3}, in which case {@code S3DocumentStorage} (R2) takes over —
 * the DB only ever holds the returned key, so nothing else changes.
 */
@Service
@ConditionalOnProperty(name = "claimlens.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalFileSystemDocumentStorage implements DocumentStorage {

    private final Path baseDir;

    public LocalFileSystemDocumentStorage(
            @Value("${claimlens.storage.local-dir:${java.io.tmpdir}/claimlens-documents}") String dir) {
        this.baseDir = Path.of(dir);
    }

    @Override
    public String store(byte[] content, String originalFileName) {
        try {
            Files.createDirectories(baseDir);
            String key = UUID.randomUUID() + "-" + sanitize(originalFileName);
            Files.write(baseDir.resolve(key), content);
            return key;
        } catch (IOException e) {
            throw new BusinessException("DOCUMENT_STORAGE_FAILED", "Could not store the document");
        }
    }

    @Override
    public byte[] retrieve(String storageKey) {
        try {
            return Files.readAllBytes(baseDir.resolve(storageKey));
        } catch (IOException e) {
            throw new BusinessException("DOCUMENT_NOT_FOUND", "Could not read the document");
        }
    }

    private static String sanitize(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "file";
        }
        return fileName.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
