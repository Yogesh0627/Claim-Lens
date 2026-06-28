package com.niyotechnologies.claimlens.organization.dto.response;

import com.niyotechnologies.claimlens.organization.enums.DepartmentStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class DepartmentResponse {

    private Long id;

    private Long tenantId;

    private String code;

    private String name;

    private String description;

    private DepartmentStatus status;

    private Instant createdAt;

    private Instant updatedAt;
}