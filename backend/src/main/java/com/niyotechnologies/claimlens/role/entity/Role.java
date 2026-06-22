package com.niyotechnologies.claimlens.role.entity;

import com.niyotechnologies.claimlens.common.entity.BaseEntity;
import com.niyotechnologies.claimlens.role.enums.RoleStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "role"
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role extends BaseEntity {

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "is_system_role", nullable = false)
    private Boolean isSystemRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private RoleStatus status;
}