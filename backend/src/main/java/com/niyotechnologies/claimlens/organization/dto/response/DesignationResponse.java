package com.niyotechnologies.claimlens.organization.dto.response;

import com.niyotechnologies.claimlens.organization.enums.DesignationStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class DesignationResponse {

    private Long id;

    private Long tenantId;

    private String code;

    private String name;

    private String description;

    private DesignationStatus status;

    private Instant createdAt;

    private Instant updatedAt;

}