package com.niyotechnologies.claimlens.product.entity;

import com.niyotechnologies.claimlens.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * A file attached to a product version — e.g. the policy-wording PDF. The bytes live in object
 * storage under {@code storageKey}; this row is metadata only. Belongs to a product version, not a
 * claim (that is the {@code document} table).
 */
@Entity
@Table(name = "product_document")
@Getter
@Setter
public class ProductDocument extends TenantAwareEntity {

    @Column(name = "insurance_product_id", nullable = false)
    private Long insuranceProductId;

    @Column(name = "insurance_product_version_id", nullable = false)
    private Long insuranceProductVersionId;

    @Column(name = "document_type", nullable = false, length = 50)
    private String documentType = "POLICY_WORDING";

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "storage_key", nullable = false, length = 512)
    private String storageKey;
}
