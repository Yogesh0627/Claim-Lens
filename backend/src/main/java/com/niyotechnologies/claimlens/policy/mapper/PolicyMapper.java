package com.niyotechnologies.claimlens.policy.mapper;

import com.niyotechnologies.claimlens.policy.dto.response.PolicyResponse;
import com.niyotechnologies.claimlens.policy.dto.response.VehicleResponse;
import com.niyotechnologies.claimlens.policy.entity.InsurancePolicy;
import com.niyotechnologies.claimlens.policy.entity.InsuredVehicle;
import org.springframework.stereotype.Component;

@Component
public class PolicyMapper {

    public PolicyResponse toResponse(InsurancePolicy p, InsuredVehicle vehicle) {
        return new PolicyResponse(
                p.getId(), p.getPublicId(), p.getPolicyNumber(),
                p.getCustomerId(), p.getInsuranceProductId(), p.getInsuranceProductVersionId(),
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
}
