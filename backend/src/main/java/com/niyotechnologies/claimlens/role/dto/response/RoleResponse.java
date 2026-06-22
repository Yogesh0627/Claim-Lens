package com.niyotechnologies.claimlens.role.dto.response;

import com.niyotechnologies.claimlens.role.enums.RoleStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RoleResponse {

    private Long id;

    private String code;

    private String name;

    private String description;

    private RoleStatus status;

    private Boolean isSystemRole;
}