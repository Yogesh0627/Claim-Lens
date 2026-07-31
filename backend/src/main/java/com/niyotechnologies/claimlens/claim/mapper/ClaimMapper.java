package com.niyotechnologies.claimlens.claim.mapper;

import com.niyotechnologies.claimlens.assignment.entity.ClaimAssignment;
import com.niyotechnologies.claimlens.assignment.enums.AssignmentStatus;
import com.niyotechnologies.claimlens.assignment.repository.ClaimAssignmentRepository;
import com.niyotechnologies.claimlens.claim.dto.response.ClaimResponse;
import com.niyotechnologies.claimlens.claim.entity.Claim;
import com.niyotechnologies.claimlens.customer.entity.Customer;
import com.niyotechnologies.claimlens.customer.repository.CustomerRepository;
import com.niyotechnologies.claimlens.product.entity.InsuranceProductVersion;
import com.niyotechnologies.claimlens.product.repository.InsuranceProductRepository;
import com.niyotechnologies.claimlens.product.repository.InsuranceProductVersionRepository;
import com.niyotechnologies.claimlens.role.entity.Role;
import com.niyotechnologies.claimlens.role.repository.RoleRepository;
import com.niyotechnologies.claimlens.user.entity.AppUser;
import com.niyotechnologies.claimlens.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
     * Single-claim mapping: resolves foreign keys into human labels with per-reference lookups. Fine
     * for one claim (a handful of queries); for a whole page use {@link #toResponses(List)}, which
     * batch-loads to avoid the N+1 (see that method).
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

        return assemble(c, customerName, customerNumber, raisedByName, raisedByCode, raisedByStaff,
                officerName, officerCode, productName, versionNumber, warnings);
    }

    /**
     * Batch mapping for a page of claims — the fix for the N+1 that the single {@link #toResponse}
     * would cause if looped over a list. Instead of ~6 lookups per claim (product version, product,
     * customer, creator, role, assignment/officer), we collect every referenced id across the whole
     * page and load each type in ONE {@code IN (...)} query, then map from in-memory maps. So a page
     * of any size costs a small, constant number of queries (~6) instead of {@code 1 + N×6}. All
     * batch queries stay {@code @TenantId}-scoped and soft-delete-filtered, so scoping is unchanged.
     */
    public List<ClaimResponse> toResponses(List<Claim> claims) {
        if (claims == null || claims.isEmpty()) {
            return List.of();
        }

        // 1. Gather the distinct ids referenced across the whole page.
        Set<Long> versionIds = new HashSet<>();
        Set<Long> customerIds = new HashSet<>();
        Set<Long> claimIds = new HashSet<>();
        Set<Long> userIds = new HashSet<>();
        for (Claim c : claims) {
            if (c.getInsuranceProductVersionId() != null) versionIds.add(c.getInsuranceProductVersionId());
            if (c.getCustomerId() != null) customerIds.add(c.getCustomerId());
            if (c.getId() != null) claimIds.add(c.getId());
            if (c.getCreatedBy() != null) userIds.add(c.getCreatedBy());
        }

        // 2. One query per referenced type.
        Map<Long, InsuranceProductVersion> versions = versionIds.isEmpty() ? Map.of()
                : productVersionRepository.findAllByIdInAndIsDeletedFalse(versionIds).stream()
                        .collect(Collectors.toMap(InsuranceProductVersion::getId, v -> v));
        Set<Long> productIds = versions.values().stream()
                .map(InsuranceProductVersion::getInsuranceProductId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> productNameById = productIds.isEmpty() ? Map.of()
                : productRepository.findAllByIdInAndIsDeletedFalse(productIds).stream()
                        .collect(Collectors.toMap(p -> p.getId(), p -> p.getName()));
        Map<Long, Customer> customers = customerIds.isEmpty() ? Map.of()
                : customerRepository.findAllByIdInAndIsDeletedFalse(customerIds).stream()
                        .collect(Collectors.toMap(Customer::getId, cu -> cu));

        // Assignments for the whole page, grouped by claim; officers = the ASSIGNED investigators.
        Map<Long, List<ClaimAssignment>> assignmentsByClaim = claimIds.isEmpty() ? Map.of()
                : assignmentRepository.findAllByClaimIdIn(claimIds).stream()
                        .collect(Collectors.groupingBy(ClaimAssignment::getClaimId));
        assignmentsByClaim.values().stream().flatMap(List::stream)
                .filter(a -> a.getStatus() == AssignmentStatus.ASSIGNED)
                .map(ClaimAssignment::getInvestigatorUserId).filter(Objects::nonNull)
                .forEach(userIds::add);

        Map<Long, AppUser> users = userIds.isEmpty() ? Map.of()
                : appUserRepository.findAllByIdInAndIsDeletedFalse(userIds).stream()
                        .collect(Collectors.toMap(AppUser::getId, u -> u));
        // Roles are a tiny global table — load all once and index by id.
        Map<Long, String> roleCodeById = roleRepository.findAllByIsDeletedFalse().stream()
                .collect(Collectors.toMap(Role::getId, Role::getCode));

        // 3. Map each claim from the maps — no queries in this loop.
        List<ClaimResponse> out = new ArrayList<>(claims.size());
        for (Claim c : claims) {
            InsuranceProductVersion version = c.getInsuranceProductVersionId() == null ? null
                    : versions.get(c.getInsuranceProductVersionId());
            Integer versionNumber = version == null ? null : version.getVersionNumber();
            String productName = (version == null || version.getInsuranceProductId() == null) ? null
                    : productNameById.get(version.getInsuranceProductId());

            Customer customer = c.getCustomerId() == null ? null : customers.get(c.getCustomerId());
            String customerName = customer == null ? null : fullName(customer.getFirstName(), customer.getLastName());
            String customerNumber = customer == null ? null : customer.getCustomerNumber();

            String raisedByName = null;
            String raisedByCode = null;
            boolean raisedByStaff = false;
            AppUser creator = c.getCreatedBy() == null ? null : users.get(c.getCreatedBy());
            if (creator != null) {
                raisedByName = fullName(creator.getFirstName(), creator.getLastName());
                raisedByCode = creator.getEmployeeCode();
                String roleCode = creator.getRoleId() == null ? null : roleCodeById.get(creator.getRoleId());
                raisedByStaff = roleCode != null && !CUSTOMER_ROLE.equals(roleCode);
            }

            String officerName = null;
            String officerCode = null;
            AppUser officer = assignmentsByClaim.getOrDefault(c.getId(), List.of()).stream()
                    .filter(a -> a.getStatus() == AssignmentStatus.ASSIGNED)
                    .map(a -> users.get(a.getInvestigatorUserId()))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
            if (officer != null) {
                officerName = fullName(officer.getFirstName(), officer.getLastName());
                officerCode = officer.getEmployeeCode();
            }

            out.add(assemble(c, customerName, customerNumber, raisedByName, raisedByCode, raisedByStaff,
                    officerName, officerCode, productName, versionNumber, List.of()));
        }
        return out;
    }

    /** The single source of truth for the response shape — both the single and batch paths route here. */
    private ClaimResponse assemble(Claim c, String customerName, String customerNumber,
                                   String raisedByName, String raisedByCode, boolean raisedByStaff,
                                   String officerName, String officerCode,
                                   String productName, Integer versionNumber, List<String> warnings) {
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
