package com.niyotechnologies.claimlens.claim.validator;

import com.niyotechnologies.claimlens.claim.entity.Claim;
import com.niyotechnologies.claimlens.claim.repository.ClaimRepository;
import com.niyotechnologies.claimlens.common.exception.BusinessException;
import com.niyotechnologies.claimlens.policy.entity.InsurancePolicy;
import com.niyotechnologies.claimlens.policy.entity.InsuredVehicle;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Claim-intake validation, split into HARD and SOFT (see interview-prep Part 9):
 *   - HARD: the claim is certainly invalid -> reject (throw), no claim is submitted.
 *   - SOFT: the claim MIGHT be fine -> raise a signal for the investigator, never reject.
 *
 * Rejecting on "maybe" throws away legitimate claims (e.g. the repair estimate arrives later),
 * so amount-over-sum-insured is a soft signal, not a rejection.
 */
@Component
@RequiredArgsConstructor
public class ClaimSubmissionValidator {

    @Autowired
    private final ClaimRepository claimRepository;

    /** @return soft-validation signals (may be empty). Throws BusinessException on any hard failure. */
    public List<String> validate(Claim claim, InsurancePolicy policy, InsuredVehicle vehicle) {
        // ---- HARD validations (reject) ----
        if (claim.getIncidentDate().isAfter(LocalDate.now())) {
            throw new BusinessException("INCIDENT_DATE_IN_FUTURE", "Incident date cannot be in the future");
        }
        if (hasOpenDuplicate(claim)) {
            // Same policy + vehicle + loss date as another still-open claim → an accidental double-submit
            // (or a deliberate one). Prior settled claims for the loss don't block; only non-terminal ones.
            throw new BusinessException("DUPLICATE_CLAIM_EXISTS",
                    "An open claim already exists for this policy, vehicle and incident date");
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

    /**
     * True when another non-terminal claim already exists for the same policy, vehicle and incident
     * date. Vehicle is compared on the normalized registration so "MH 12 AB 1234" and "MH12AB1234"
     * count as the same. The claim being submitted is excluded (by id), and settled claims
     * (APPROVED/REJECTED/CLOSED) don't block a fresh one.
     */
    private boolean hasOpenDuplicate(Claim claim) {
        String thisReg = normalized(claim.getVehicleRegistrationNumber());
        return claimRepository
                .findAllByInsurancePolicyIdAndIncidentDateAndIsDeletedFalse(
                        claim.getInsurancePolicyId(), claim.getIncidentDate())
                .stream()
                .filter(other -> !other.getId().equals(claim.getId()))
                .filter(other -> !other.getStatus().isTerminal())
                .anyMatch(other -> Objects.equals(thisReg, normalized(other.getVehicleRegistrationNumber())));
    }

    private static String normalized(String registration) {
        return registration == null ? null : InsuredVehicle.normalizeRegistration(registration);
    }
}
