package com.niyotechnologies.claimlens.organization.service;


import com.niyotechnologies.claimlens.organization.dto.request.CreateInsuranceCompanyRequest;
import com.niyotechnologies.claimlens.organization.dto.response.InsuranceCompanyResponse;

public interface OrganizationService {

    InsuranceCompanyResponse createInsuranceCompany(
            CreateInsuranceCompanyRequest request
    );
}