package com.niyotechnologies.claimlens.organization.entity;

import com.niyotechnologies.claimlens.common.entity.BaseEntity;
import com.niyotechnologies.claimlens.organization.enums.RegionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "region",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_region_tenant_code",
                        columnNames = {"tenant_id", "code"}
                )
        }
)
@Getter
@Setter
public class Region extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "owner_user_id")
    private Long ownerUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RegionStatus status;

    @Column(columnDefinition = "TEXT")
    private String description;
}