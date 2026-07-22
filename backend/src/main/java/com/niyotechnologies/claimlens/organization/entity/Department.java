package com.niyotechnologies.claimlens.organization.entity;

import com.niyotechnologies.claimlens.common.entity.TenantAwareEntity;
import com.niyotechnologies.claimlens.organization.enums.DepartmentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "department")
@Getter
@Setter
public class Department extends TenantAwareEntity {

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DepartmentStatus status;
}