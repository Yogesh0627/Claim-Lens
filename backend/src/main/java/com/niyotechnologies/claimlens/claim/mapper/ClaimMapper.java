package com.niyotechnologies.claimlens.claim.mapper;

import com.niyotechnologies.claimlens.claim.dto.response.ClaimResponse;
import com.niyotechnologies.claimlens.claim.entity.Claim;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ClaimMapper {

    public ClaimResponse toResponse(Claim c, List<String> warnings) {
        return new ClaimResponse(
                c.getId(), c.getPublicId(), c.getClaimNumber(), c.getCustomerId(),
                c.getInsurancePolicyId(), c.getInsuranceProductVersionId(), c.getPolicyNumber(),
                c.getVehicleRegistrationNumber(), c.getIncidentDate(), c.getClaimAmount(),
                c.getStatus(), c.getSubmittedAt(),
                warnings == null ? List.of() : warnings);
    }
}
