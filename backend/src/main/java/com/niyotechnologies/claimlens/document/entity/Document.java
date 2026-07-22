package com.niyotechnologies.claimlens.document.entity;

import com.niyotechnologies.claimlens.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

/** A claim document. Metadata only; the bytes live in object storage under storageKey. */
@Entity
@Table(name = "document")
@Getter
@Setter
public class Document extends TenantAwareEntity {

    @UuidGenerator
    @Column(name = "public_id", nullable = false, updatable = false)
    private UUID publicId;

    @Column(name = "claim_id")
    private Long claimId;

    @Column(name = "document_type", nullable = false, length = 50)
    private String documentType;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "storage_key", nullable = false, length = 512)
    private String storageKey;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "uploaded_by")
    private Long uploadedBy;

    /** Points at the live {@link DocumentVersion}. The mirror fields above snapshot that version. */
    @Column(name = "current_version_id")
    private Long currentVersionId;
}
