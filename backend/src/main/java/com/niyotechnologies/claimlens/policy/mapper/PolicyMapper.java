package com.niyotechnologies.claimlens.policy.mapper;

import com.niyotechnologies.claimlens.customer.repository.CustomerRepository;
import com.niyotechnologies.claimlens.policy.dto.response.PolicyResponse;
import com.niyotechnologies.claimlens.policy.dto.response.VehicleResponse;
import com.niyotechnologies.claimlens.policy.entity.InsurancePolicy;
import com.niyotechnologies.claimlens.policy.entity.InsuredVehicle;
import com.niyotechnologies.claimlens.product.entity.InsuranceProduct;
import com.niyotechnologies.claimlens.product.entity.InsuranceProductVersion;
import com.niyotechnologies.claimlens.product.repository.InsuranceProductRepository;
import com.niyotechnologies.claimlens.product.repository.InsuranceProductVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PolicyMapper {

    @Autowired
    private final CustomerRepository customerRepository;
    @Autowired
    private final InsuranceProductRepository productRepository;
    @Autowired
    private final InsuranceProductVersionRepository productVersionRepository;

    /**
     * Builds the response, resolving the policyholder name and product/version so the UI shows
     * "Demo Motor Comprehensive v1 — Rahul Sharma" rather than raw foreign-key ids. Resolution lives
     * here so every caller (staff policy view and the customer portal alike) is enriched identically.
     * Lookups are tenant-scoped and null-safe — a missing reference just leaves that field null.
     */
    public PolicyResponse toResponse(InsurancePolicy p, InsuredVehicle vehicle) {
        String customerName = customerRepository.findByIdAndIsDeletedFalse(p.getCustomerId())
                .map(c -> fullName(c.getFirstName(), c.getLastName()))
                .orElse(null);

        String productName = null;
        String productCode = null;
        InsuranceProduct product = productRepository
                .findByIdAndIsDeletedFalse(p.getInsuranceProductId()).orElse(null);
        if (product != null) {
            productName = product.getName();
            productCode = product.getCode();
        }

        Integer versionNumber = productVersionRepository
                .findByIdAndIsDeletedFalse(p.getInsuranceProductVersionId())
                .map(InsuranceProductVersion::getVersionNumber)
                .orElse(null);

        return new PolicyResponse(
                p.getId(), p.getPublicId(), p.getPolicyNumber(),
                p.getCustomerId(), customerName,
                p.getInsuranceProductId(), productName, productCode,
                p.getInsuranceProductVersionId(), versionNumber,
                p.getEffectiveFrom(), p.getEffectiveTo(),
                p.getSumInsured(), p.getDeductible(), p.getPremiumAmount(), p.getCurrency(),
                p.getStatus(), p.getIssuedAt(),
                vehicle == null ? null : toVehicleResponse(vehicle));
    }

    public VehicleResponse toVehicleResponse(InsuredVehicle v) {
        return new VehicleResponse(
                v.getId(), v.getRegistrationNumber(), v.getMake(), v.getModel(), v.getVariant(),
                v.getManufactureYear(), v.getChassisNumber(), v.getEngineNumber(),
                v.getColour(), v.getFuelType(), v.getSeatingCapacity(), v.getIdv(), v.getStatus());
    }

    private static String fullName(String first, String last) {
        String name = ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim();
        return name.isEmpty() ? null : name;
    }
}
