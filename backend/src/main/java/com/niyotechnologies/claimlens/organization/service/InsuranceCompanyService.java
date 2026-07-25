package com.niyotechnologies.claimlens.organization.service;


import com.niyotechnologies.claimlens.organization.dto.request.CreateInsuranceCompanyRequest;
import com.niyotechnologies.claimlens.organization.dto.request.UpdateInsuranceCompanyRequest;
import com.niyotechnologies.claimlens.organization.dto.response.InsuranceCompanyResponse;

import java.util.List;

public interface InsuranceCompanyService {

    InsuranceCompanyResponse createInsuranceCompany(
            CreateInsuranceCompanyRequest request
    );

    InsuranceCompanyResponse getInsuranceCompanyById(
            Long companyId
    );

    InsuranceCompanyResponse getMyCompany();

    List<InsuranceCompanyResponse> getAllCompanies();

    InsuranceCompanyResponse updateInsuranceCompany(
            Long companyId,
            UpdateInsuranceCompanyRequest request
    );

    void deleteInsuranceCompany(
            Long companyId
    );
}
