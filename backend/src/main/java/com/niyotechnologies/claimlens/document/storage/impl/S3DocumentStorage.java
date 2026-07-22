package com.niyotechnologies.claimlens.document.storage.impl;

import com.niyotechnologies.claimlens.common.exception.BusinessException;
import com.niyotechnologies.claimlens.document.storage.DocumentStorage;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

/**
 * S3-compatible object storage — used for Cloudflare R2 (and AWS S3). Active when
 * {@code claimlens.storage.provider=s3}. R2 needs path-style access and region "auto"; the endpoint is
 * the account's S3 API endpoint. The DB stores only the returned object key.
 */
@Service
@ConditionalOnProperty(name = "claimlens.storage.provider", havingValue = "s3")
public class S3DocumentStorage implements DocumentStorage {

    private final S3Client client;
    private final String bucket;
    private final String publicBaseUrl;

    public S3DocumentStorage(
            @Value("${claimlens.storage.s3.endpoint}") String endpoint,
            @Value("${claimlens.storage.s3.region:auto}") String region,
            @Value("${claimlens.storage.s3.bucket}") String bucket,
            @Value("${claimlens.storage.s3.access-key}") String accessKey,
            @Value("${claimlens.storage.s3.secret-key}") String secretKey,
            @Value("${claimlens.storage.s3.public-base-url:}") String publicBaseUrl) {
        if (endpoint == null || endpoint.isBlank() || bucket == null || bucket.isBlank()) {
            throw new IllegalStateException(
                    "claimlens.storage.provider=s3 requires storage.s3.endpoint and storage.s3.bucket");
        }
        this.bucket = bucket;
        this.publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.replaceAll("/+$", "");
        this.client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .httpClient(UrlConnectionHttpClient.create())
                .build();
    }

    @Override
    public String store(byte[] content, String originalFileName) {
        String key = "documents/" + UUID.randomUUID() + "-" + sanitize(originalFileName);
        try {
            client.putObject(PutObjectRequest.builder().bucket(bucket).key(key).build(),
                    RequestBody.fromBytes(content));
            return key;
        } catch (Exception e) {
            throw new BusinessException("DOCUMENT_STORAGE_FAILED", "Could not store the document");
        }
    }

    @Override
    public Optional<String> publicUrl(String storageKey) {
        if (publicBaseUrl.isBlank() || storageKey == null || storageKey.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(publicBaseUrl + "/" + storageKey);
    }

    @Override
    public byte[] retrieve(String storageKey) {
        try {
            return client.getObjectAsBytes(
                    GetObjectRequest.builder().bucket(bucket).key(storageKey).build()).asByteArray();
        } catch (NoSuchKeyException e) {
            throw new BusinessException("DOCUMENT_NOT_FOUND", "Document not found in storage");
        } catch (Exception e) {
            throw new BusinessException("DOCUMENT_NOT_FOUND", "Could not read the document");
        }
    }

    private static String sanitize(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "file";
        }
        return fileName.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    @PreDestroy
    void close() {
        client.close();
    }
}
