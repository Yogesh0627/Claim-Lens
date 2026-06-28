package com.niyotechnologies.claimlens.organization.mapper;

import com.niyotechnologies.claimlens.organization.dto.request.CreateDesignationRequest;
import com.niyotechnologies.claimlens.organization.dto.request.UpdateDesignationRequest;
import com.niyotechnologies.claimlens.organization.dto.response.DesignationResponse;
import com.niyotechnologies.claimlens.organization.entity.Designation;
import com.niyotechnologies.claimlens.organization.enums.DesignationStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DesignationMapper {

    public Designation toEntity(
            Long companyId,
            CreateDesignationRequest request
    ) {

        Designation designation = new Designation();

        designation.setTenantId(companyId);
        designation.setCode(request.getCode().trim());
        designation.setName(request.getName().trim());
        designation.setDescription(trim(request.getDescription()));
        designation.setStatus(DesignationStatus.ACTIVE);

        return designation;
    }

    public void updateEntity(
            Designation designation,
            UpdateDesignationRequest request
    ) {

        designation.setName(request.getName().trim());
        designation.setDescription(trim(request.getDescription()));
    }

    public DesignationResponse toResponse(
            Designation designation
    ) {

        return DesignationResponse.builder()
                .id(designation.getId())
                .tenantId(designation.getTenantId())
                .code(designation.getCode())
                .name(designation.getName())
                .description(designation.getDescription())
                .status(designation.getStatus())
                .createdAt(designation.getCreatedAt())
                .updatedAt(designation.getUpdatedAt())
                .build();
    }

    public List<DesignationResponse> toResponseList(
            List<Designation> designations
    ) {

        return designations.stream()
                .map(this::toResponse)
                .toList();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}