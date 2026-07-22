package com.niyotechnologies.claimlens.document.entity;

import com.niyotechnologies.claimlens.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * One uploaded revision of a {@link Document}. Immutable once written — a re-upload creates a new
 * version rather than overwriting this one, preserving the full history a claim audit needs.
 */
@Entity
@Table(name = "document_version")
@Getter
@Setter
public class DocumentVersion extends TenantAwareEntity {

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

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
}
