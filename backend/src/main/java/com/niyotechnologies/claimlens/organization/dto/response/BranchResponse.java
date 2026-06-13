package com.niyotechnologies.claimlens.organization.dto.response;

import com.niyotechnologies.claimlens.organization.enums.BranchStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class BranchResponse {

    private Long id;

    private Long tenantId;

    private Long regionId;

    private String code;

    private String name;

    private Long ownerUserId;

    private String email;

    private String phone;

    private String address;

    private String city;

    private String state;

    private String country;

    private String postalCode;

    private BranchStatus status;

    private String description;

    private Instant createdAt;

    private Instant updatedAt;
}