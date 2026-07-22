package com.niyotechnologies.claimlens.organization.service;

import com.niyotechnologies.claimlens.organization.dto.request.CreateDesignationRequest;
import com.niyotechnologies.claimlens.organization.dto.request.UpdateDesignationRequest;
import com.niyotechnologies.claimlens.organization.dto.response.DesignationResponse;

import java.util.List;

public interface DesignationService {

    DesignationResponse createDesignation(
            CreateDesignationRequest request
    );

    DesignationResponse getDesignationById(
            Long designationId
    );

    List<DesignationResponse> getAllDesignations();

    DesignationResponse updateDesignation(
            Long designationId,
            UpdateDesignationRequest request
    );

    void deleteDesignation(
            Long designationId
    );
}
