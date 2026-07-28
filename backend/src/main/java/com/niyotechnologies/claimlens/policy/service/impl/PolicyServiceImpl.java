package com.niyotechnologies.claimlens.policy.service.impl;

import com.niyotechnologies.claimlens.common.exception.BusinessException;
import com.niyotechnologies.claimlens.common.exception.NotFoundException;
import com.niyotechnologies.claimlens.common.response.PagedResponse;
import com.niyotechnologies.claimlens.common.util.PageRequests;
import org.springframework.data.domain.Sort;
import com.niyotechnologies.claimlens.customer.repository.CustomerRepository;
import com.niyotechnologies.claimlens.policy.dto.request.CreatePolicyRequest;
import com.niyotechnologies.claimlens.policy.dto.request.VehicleRequest;
import com.niyotechnologies.claimlens.policy.dto.response.PolicyResponse;
import com.niyotechnologies.claimlens.policy.entity.InsurancePolicy;
import com.niyotechnologies.claimlens.policy.entity.InsuredVehicle;
import com.niyotechnologies.claimlens.policy.enums.PolicyStatus;
import com.niyotechnologies.claimlens.policy.enums.VehicleStatus;
import com.niyotechnologies.claimlens.policy.mapper.PolicyMapper;
import com.niyotechnologies.claimlens.policy.repository.InsurancePolicyRepository;
import com.niyotechnologies.claimlens.policy.repository.InsuredVehicleRepository;
import com.niyotechnologies.claimlens.product.entity.InsuranceProduct;
import com.niyotechnologies.claimlens.product.entity.InsuranceProductVersion;
import com.niyotechnologies.claimlens.product.enums.ProductVersionStatus;
import com.niyotechnologies.claimlens.product.repository.InsuranceProductRepository;
import com.niyotechnologies.claimlens.product.repository.InsuranceProductVersionRepository;
import com.niyotechnologies.claimlens.policy.service.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * The insurance contract. The load-bearing rule lives in createPolicy: the policy is PINNED to the
 * product's currently ACTIVE version at sale. A later claim inherits this pinned version, so it is
 * judged against the terms the customer agreed to — never the product's current version.
 */
@Service
@RequiredArgsConstructor
public class PolicyServiceImpl implements PolicyService {

    @Autowired
    private final InsurancePolicyRepository policyRepository;
    @Autowired
    private final InsuredVehicleRepository vehicleRepository;
    @Autowired
    private final InsuranceProductRepository productRepository;
    @Autowired
    private final InsuranceProductVersionRepository productVersionRepository;
    @Autowired
    private final CustomerRepository customerRepository;
    @Autowired
    private final PolicyMapper policyMapper;

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('POLICY_WRITE')")
    public PolicyResponse createPolicy(CreatePolicyRequest request) {
        if (policyRepository.existsByPolicyNumberAndIsDeletedFalse(request.policyNumber())) {
            throw new BusinessException("POLICY_NUMBER_ALREADY_EXISTS", "Policy number already exists");
        }

        InsuranceProduct product = productRepository
                .findByIdAndIsDeletedFalse(request.insuranceProductId())
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not found"));

        // Pin to the product's ACTIVE version at sale time.
        InsuranceProductVersion activeVersion = productVersionRepository
                .findByInsuranceProductIdAndStatusAndIsDeletedFalse(product.getId(), ProductVersionStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(
                        "PRODUCT_HAS_NO_ACTIVE_VERSION",
                        "Product has no active version to sell"));

        // Customer must exist within this tenant (tenant-scoped lookup).
        customerRepository.findByIdAndIsDeletedFalse(request.customerId())
                .orElseThrow(() -> new NotFoundException("CUSTOMER_NOT_FOUND", "Customer not found"));

        InsurancePolicy policy = new InsurancePolicy();
        policy.setPolicyNumber(request.policyNumber().trim());
        policy.setCustomerId(request.customerId());
        policy.setInsuranceProductId(product.getId());
        policy.setInsuranceProductVersionId(activeVersion.getId());   // <-- version pinning
        policy.setEffectiveFrom(request.effectiveFrom());
        policy.setEffectiveTo(request.effectiveTo());
        policy.setSumInsured(request.sumInsured());
        policy.setDeductible(request.deductible() == null ? BigDecimal.ZERO : request.deductible());
        policy.setPremiumAmount(request.premiumAmount());
        policy.setCurrency(request.currency() == null ? "INR" : request.currency());
        policy.setStatus(PolicyStatus.ACTIVE);
        policy.setIssuedAt(Instant.now());
        InsurancePolicy savedPolicy = policyRepository.save(policy);

        InsuredVehicle vehicle = toVehicle(request.vehicle(), savedPolicy.getId());
        InsuredVehicle savedVehicle = vehicleRepository.save(vehicle);

        return policyMapper.toResponse(savedPolicy, savedVehicle);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('POLICY_READ')")
    public PolicyResponse getPolicy(Long policyId) {
        InsurancePolicy policy = policyRepository.findByIdAndIsDeletedFalse(policyId)
                .orElseThrow(() -> new NotFoundException("POLICY_NOT_FOUND", "Policy not found"));
        InsuredVehicle vehicle = vehicleRepository
                .findAllByInsurancePolicyIdAndIsDeletedFalse(policy.getId())
                .stream().findFirst().orElse(null);
        return policyMapper.toResponse(policy, vehicle);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('POLICY_WRITE')")
    public PolicyResponse cancelPolicy(Long policyId) {
        InsurancePolicy policy = policyRepository.findByIdAndIsDeletedFalse(policyId)
                .orElseThrow(() -> new NotFoundException("POLICY_NOT_FOUND", "Policy not found"));
        policy.setStatus(PolicyStatus.CANCELLED);
        InsurancePolicy saved = policyRepository.save(policy);
        InsuredVehicle vehicle = vehicleRepository
                .findAllByInsurancePolicyIdAndIsDeletedFalse(saved.getId())
                .stream().findFirst().orElse(null);
        return policyMapper.toResponse(saved, vehicle);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('POLICY_READ')")
    public PagedResponse<PolicyResponse> getPolicies(int page, int size) {
        var pageable = PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        return PagedResponse.from(
                policyRepository.findAllByIsDeletedFalse(pageable), this::toResponseWithVehicle);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('POLICY_READ')")
    public List<PolicyResponse> getPolicyOptions() {
        return policyRepository.findAllByIsDeletedFalse().stream()
                .map(this::toResponseWithVehicle)
                .toList();
    }

    private PolicyResponse toResponseWithVehicle(InsurancePolicy policy) {
        return policyMapper.toResponse(policy,
                vehicleRepository.findAllByInsurancePolicyIdAndIsDeletedFalse(policy.getId())
                        .stream().findFirst().orElse(null));
    }

    private InsuredVehicle toVehicle(VehicleRequest request, Long policyId) {
        InsuredVehicle vehicle = new InsuredVehicle();
        vehicle.setInsurancePolicyId(policyId);
        vehicle.setRegistrationNumber(request.registrationNumber().trim());
        vehicle.setRegistrationNumberNormalized(
                InsuredVehicle.normalizeRegistration(request.registrationNumber()));
        vehicle.setMake(request.make().trim());
        vehicle.setModel(request.model().trim());
        vehicle.setVariant(request.variant());
        vehicle.setManufactureYear(request.manufactureYear());
        vehicle.setChassisNumber(request.chassisNumber());
        vehicle.setEngineNumber(request.engineNumber());
        vehicle.setColour(request.colour());
        vehicle.setFuelType(request.fuelType());
        vehicle.setSeatingCapacity(request.seatingCapacity());
        vehicle.setIdv(request.idv());
        vehicle.setStatus(VehicleStatus.ACTIVE);
        return vehicle;
    }
}
