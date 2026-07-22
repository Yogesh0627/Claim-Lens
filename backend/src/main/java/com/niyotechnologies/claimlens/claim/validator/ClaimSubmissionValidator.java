package com.niyotechnologies.claimlens.claim.validator;

import com.niyotechnologies.claimlens.claim.entity.Claim;
import com.niyotechnologies.claimlens.common.exception.BusinessException;
import com.niyotechnologies.claimlens.policy.entity.InsurancePolicy;
import com.niyotechnologies.claimlens.policy.entity.InsuredVehicle;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Claim-intake validation, split into HARD and SOFT (see interview-prep Part 9):
 *   - HARD: the claim is certainly invalid -> reject (throw), no claim is submitted.
 *   - SOFT: the claim MIGHT be fine -> raise a signal for the investigator, never reject.
 *
 * Rejecting on "maybe" throws away legitimate claims (e.g. the repair estimate arrives later),
 * so amount-over-sum-insured is a soft signal, not a rejection.
 */
@Component
public class ClaimSubmissionValidator {

    /** @return soft-validation signals (may be empty). Throws BusinessException on any hard failure. */
    public List<String> validate(Claim claim, InsurancePolicy policy, InsuredVehicle vehicle) {
        // ---- HARD validations (reject) ----
        if (claim.getIncidentDate().isAfter(LocalDate.now())) {
            throw new BusinessException("INCIDENT_DATE_IN_FUTURE", "Incident date cannot be in the future");
        }
        if (!policy.isActiveOn(claim.getIncidentDate())) {
            throw new BusinessException("POLICY_NOT_ACTIVE_ON_LOSS_DATE",
                    "The policy was not active on the incident date");
        }
        if (!claim.getCustomerId().equals(policy.getCustomerId())) {
            throw new BusinessException("CLAIMANT_NOT_POLICYHOLDER",
                    "The claimant is not the policyholder");
        }
        if (claim.getVehicleRegistrationNumber() != null && vehicle != null) {
            String claimReg = InsuredVehicle.normalizeRegistration(claim.getVehicleRegistrationNumber());
            if (!vehicle.getRegistrationNumberNormalized().equals(claimReg)) {
                throw new BusinessException("VEHICLE_NOT_COVERED_BY_POLICY",
                        "The vehicle on the claim does not match the policy");
            }
        }

        // ---- SOFT validations (signal, never reject) ----
        List<String> signals = new ArrayList<>();
        if (claim.getClaimAmount() != null && policy.getSumInsured() != null
                && claim.getClaimAmount().compareTo(policy.getSumInsured()) > 0) {
            // NOTE (V1 simplification): sum insured is an aggregate annual limit; the correct check
            // subtracts prior approved claims in the period. Shipped as a soft signal only.
            signals.add("POTENTIAL_OVER_LIMIT_CLAIM");
        }
        return signals;
    }
}
