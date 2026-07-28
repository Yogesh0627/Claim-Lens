package com.niyotechnologies.claimlens.claim.mapper;

import com.niyotechnologies.claimlens.assignment.enums.AssignmentStatus;
import com.niyotechnologies.claimlens.assignment.repository.ClaimAssignmentRepository;
import com.niyotechnologies.claimlens.claim.dto.response.ClaimResponse;
import com.niyotechnologies.claimlens.claim.entity.Claim;
import com.niyotechnologies.claimlens.customer.entity.Customer;
import com.niyotechnologies.claimlens.customer.repository.CustomerRepository;
import com.niyotechnologies.claimlens.product.entity.InsuranceProductVersion;
import com.niyotechnologies.claimlens.product.repository.InsuranceProductRepository;
import com.niyotechnologies.claimlens.product.repository.InsuranceProductVersionRepository;
import com.niyotechnologies.claimlens.role.repository.RoleRepository;
import com.niyotechnologies.claimlens.user.entity.AppUser;
import com.niyotechnologies.claimlens.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ClaimMapper {

    private static final String CUSTOMER_ROLE = "CUSTOMER";

    @Autowired
    private final InsuranceProductVersionRepository productVersionRepository;
    @Autowired
    private final InsuranceProductRepository productRepository;
    @Autowired
    private final CustomerRepository customerRepository;
    @Autowired
    private final AppUserRepository appUserRepository;
    @Autowired
    private final RoleRepository roleRepository;
    @Autowired
    private final ClaimAssignmentRepository assignmentRepository;

    /**
     * Builds the response, resolving foreign keys into human labels: the pinned product version →
     * "Demo Motor Comprehensive · v1"; the policyholder → name + customer number; and the creator →
     * who actually filed it (the customer, or a staff member filing on their behalf). All lookups are
     * tenant-scoped and null-safe — a missing reference just leaves that field null.
     */
    public ClaimResponse toResponse(Claim c, List<String> warnings) {
        String productName = null;
        Integer versionNumber = null;
        InsuranceProductVersion version = c.getInsuranceProductVersionId() == null ? null
                : productVersionRepository.findByIdAndIsDeletedFalse(c.getInsuranceProductVersionId())
                        .orElse(null);
        if (version != null) {
            versionNumber = version.getVersionNumber();
            productName = productRepository.findByIdAndIsDeletedFalse(version.getInsuranceProductId())
                    .map(p -> p.getName())
                    .orElse(null);
        }

        String customerName = null;
        String customerNumber = null;
        if (c.getCustomerId() != null) {
            Customer customer = customerRepository.findByIdAndIsDeletedFalse(c.getCustomerId())
                    .orElse(null);
            if (customer != null) {
                customerName = fullName(customer.getFirstName(), customer.getLastName());
                customerNumber = customer.getCustomerNumber();
            }
        }

        String raisedByName = null;
        String raisedByCode = null;
        boolean raisedByStaff = false;
        if (c.getCreatedBy() != null) {
            AppUser creator = appUserRepository.findByIdAndIsDeletedFalse(c.getCreatedBy()).orElse(null);
            if (creator != null) {
                raisedByName = fullName(creator.getFirstName(), creator.getLastName());
                raisedByCode = creator.getEmployeeCode();
                String roleCode = creator.getRoleId() == null ? null
                        : roleRepository.findByIdAndIsDeletedFalse(creator.getRoleId())
                                .map(r -> r.getCode()).orElse(null);
                // Anyone other than the policyholder themselves counts as "filed by staff".
                raisedByStaff = roleCode != null && !CUSTOMER_ROLE.equals(roleCode);
            }
        }

        // The current investigating officer — the live (ASSIGNED) assignment, if any.
        String officerName = null;
        String officerCode = null;
        AppUser officer = assignmentRepository.findAllByClaimId(c.getId()).stream()
                .filter(a -> a.getStatus() == AssignmentStatus.ASSIGNED)
                .map(a -> appUserRepository.findByIdAndIsDeletedFalse(a.getInvestigatorUserId()).orElse(null))
                .filter(u -> u != null)
                .findFirst()
                .orElse(null);
        if (officer != null) {
            officerName = fullName(officer.getFirstName(), officer.getLastName());
            officerCode = officer.getEmployeeCode();
        }

        return new ClaimResponse(
                c.getId(), c.getPublicId(), c.getClaimNumber(), c.getCustomerId(),
                customerName, customerNumber,
                raisedByName, raisedByCode, raisedByStaff,
                officerName, officerCode,
                c.getInsurancePolicyId(), c.getInsuranceProductVersionId(),
                productName, versionNumber,
                c.getPolicyNumber(),
                c.getVehicleRegistrationNumber(), c.getIncidentDate(), c.getClaimAmount(),
                c.getStatus(), c.getSubmittedAt(),
                warnings == null ? List.of() : warnings);
    }

    private static String fullName(String first, String last) {
        String name = ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim();
        return name.isEmpty() ? null : name;
    }
}
