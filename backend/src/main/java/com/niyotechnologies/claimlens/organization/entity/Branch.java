package com.niyotechnologies.claimlens.organization.entity;

import com.niyotechnologies.claimlens.common.entity.TenantAwareEntity;
import com.niyotechnologies.claimlens.organization.enums.BranchStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "branch",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_branch_tenant_code",
                        columnNames = {"tenant_id", "code"}
                )
        }
)
@Getter
@Setter
public class Branch extends TenantAwareEntity {

    @Column(name = "region_id", nullable = false)
    private Long regionId;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "owner_user_id")
    private Long ownerUserId;

    @Column(length = 255)
    private String email;

    @Column(length = 50)
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(length = 100)
    private String country;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private BranchStatus status;

    @Column(columnDefinition = "TEXT")
    private String description;
}