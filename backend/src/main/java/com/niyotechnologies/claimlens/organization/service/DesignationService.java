package com.niyotechnologies.claimlens.organization.service;

import com.niyotechnologies.claimlens.organization.dto.request.CreateDesignationRequest;
import com.niyotechnologies.claimlens.organization.dto.request.UpdateDesignationRequest;
import com.niyotechnologies.claimlens.organization.dto.response.DesignationResponse;

import java.util.List;

public interface DesignationService {

    DesignationResponse createDesignation(
            Long companyId,
            CreateDesignationRequest request
    );

    DesignationResponse getDesignationById(
            Long companyId,
            Long designationId
    );

    List<DesignationResponse> getAllDesignations(
            Long companyId
    );

    DesignationResponse updateDesignation(
            Long companyId,
            Long designationId,
            UpdateDesignationRequest request
    );

    void deleteDesignation(
            Long companyId,
            Long designationId
    );
}