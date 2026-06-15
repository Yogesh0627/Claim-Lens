package com.niyotechnologies.claimlens.organization.dto.response;

import com.niyotechnologies.claimlens.organization.enums.RegionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class RegionResponse {

    private Long id;

    private Long tenantId;

    private String code;

    private String name;

    private Long ownerUserId;

    private RegionStatus status;

    private String description;

    private Instant createdAt;

    private Instant updatedAt;
}
