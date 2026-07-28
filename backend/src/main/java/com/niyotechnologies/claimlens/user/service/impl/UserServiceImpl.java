package com.niyotechnologies.claimlens.user.service.impl;

import com.niyotechnologies.claimlens.auth.enums.InvitationPurpose;
import com.niyotechnologies.claimlens.auth.service.InvitationService;
import com.niyotechnologies.claimlens.common.exception.BusinessException;
import com.niyotechnologies.claimlens.common.exception.NotFoundException;
import com.niyotechnologies.claimlens.common.response.PagedResponse;
import com.niyotechnologies.claimlens.common.util.PageRequests;
import com.niyotechnologies.claimlens.customer.repository.CustomerRepository;
import com.niyotechnologies.claimlens.organization.entity.Branch;
import com.niyotechnologies.claimlens.organization.entity.Department;
import com.niyotechnologies.claimlens.organization.entity.Designation;
import com.niyotechnologies.claimlens.organization.entity.Region;
import com.niyotechnologies.claimlens.organization.repository.BranchRepository;
import com.niyotechnologies.claimlens.organization.repository.DepartmentRepository;
import com.niyotechnologies.claimlens.organization.repository.DesignationRepository;
import com.niyotechnologies.claimlens.organization.repository.RegionRepository;
import com.niyotechnologies.claimlens.role.entity.Role;
import com.niyotechnologies.claimlens.role.repository.RoleRepository;
import com.niyotechnologies.claimlens.user.dto.CreateUserRequest;
import com.niyotechnologies.claimlens.user.dto.UpdateUserRequest;
import com.niyotechnologies.claimlens.user.dto.UserResponse;
import com.niyotechnologies.claimlens.user.entity.AppUser;
import com.niyotechnologies.claimlens.user.entity.UserBranchAssignment;
import com.niyotechnologies.claimlens.user.enums.AuthProvider;
import com.niyotechnologies.claimlens.user.enums.UserStatus;
import com.niyotechnologies.claimlens.user.repository.AppUserRepository;
import com.niyotechnologies.claimlens.user.repository.UserBranchAssignmentRepository;
import com.niyotechnologies.claimlens.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    /** The seeded role whose users are policyholders rather than staff. */
    private static final String CUSTOMER_ROLE_CODE = "CUSTOMER";

    @Autowired
    private final AppUserRepository appUserRepository;
    @Autowired
    private final RoleRepository roleRepository;
    @Autowired
    private final PasswordEncoder passwordEncoder;
    @Autowired
    private final CustomerRepository customerRepository;
    @Autowired
    private final InvitationService invitationService;
    @Autowired
    private final DepartmentRepository departmentRepository;
    @Autowired
    private final DesignationRepository designationRepository;
    @Autowired
    private final BranchRepository branchRepository;
    @Autowired
    private final RegionRepository regionRepository;
    @Autowired
    private final UserBranchAssignmentRepository userBranchAssignmentRepository;

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('USER_READ')")
    public PagedResponse<UserResponse> list(int page, int size) {
        // role/permission are global (no @TenantId) — safe to read regardless of tenant.
        Map<Long, String> roleCodeById = roleRepository.findAllByIsDeletedFalse().stream()
                .collect(Collectors.toMap(Role::getId, Role::getCode));
        var pageable = PageRequests.of(page, size, Sort.by("firstName").ascending());
        return PagedResponse.from(
                appUserRepository.findAllByIsDeletedFalse(pageable),
                u -> toResponse(u, roleCodeById));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('USER_READ')")
    public List<UserResponse> options(String roleCode) {
        // role/permission are global (no @TenantId) — safe to read regardless of tenant.
        Map<Long, String> roleCodeById = roleRepository.findAllByIsDeletedFalse().stream()
                .collect(Collectors.toMap(Role::getId, Role::getCode));

        List<AppUser> users;
        if (roleCode != null && !roleCode.isBlank()) {
            Role role = roleRepository.findByCodeAndIsDeletedFalse(roleCode)
                    .orElseThrow(() -> new NotFoundException("ROLE_NOT_FOUND", "Role not found"));
            users = appUserRepository.findAllByRoleIdAndIsDeletedFalseOrderByFirstNameAsc(role.getId());
        } else {
            users = appUserRepository.findAllByIsDeletedFalseOrderByFirstNameAsc();
        }

        return users.stream()
                .map(u -> toResponse(u, roleCodeById))
                .toList();
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('USER_WRITE')")
    public UserResponse create(CreateUserRequest request) {
        // Email is globally unique — the native lookup crosses tenants to enforce it.
        appUserRepository.findByEmailForAuthentication(request.email()).ifPresent(u -> {
            throw new BusinessException("EMAIL_EXISTS", "A user with this email already exists");
        });
        Role role = requireRole(request.roleCode());

        Long customerId = resolveCustomerId(request, role);

        AppUser user = new AppUser();
        user.setEmail(request.email());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmployeeCode(resolveEmployeeCode(request, role, customerId));
        user.setPhone(request.phone());
        user.setRoleId(role.getId());
        user.setCustomerId(customerId);
        // Org placement only applies to staff — a CUSTOMER (policyholder) has none.
        boolean isStaff = !CUSTOMER_ROLE_CODE.equals(role.getCode());
        if (isStaff) {
            user.setDepartmentId(request.departmentId());
            user.setDesignationId(request.designationId());
            user.setRegionId(request.regionId());
            user.setHomeBranchId(request.homeBranchId());
        }
        user.setAuthProvider(AuthProvider.LOCAL);
        if (StringUtils.hasText(request.password())) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
            user.setStatus(UserStatus.ACTIVE);
        } else {
            user.setStatus(UserStatus.INVITED);
        }
        // tenant_id is stamped automatically by @TenantId on insert.
        AppUser saved = appUserRepository.save(user);
        if (isStaff) {
            replaceBranchAssignments(saved.getId(), request.branchIds(), request.homeBranchId());
        }

        // No password supplied -> the account is INVITED and unusable until the person sets one, so
        // email them a single-use link. Without this, INVITED was a dead end.
        if (saved.getStatus() == UserStatus.INVITED) {
            invitationService.invite(saved, InvitationPurpose.INVITE);
        }
        return toResponse(saved, roleCodeMap());
    }

    /**
     * Replaces a user's branch assignments with the given set (idempotent). The home branch is folded
     * in and flagged primary, so "home branch" and the branch list stay consistent.
     */
    private void replaceBranchAssignments(Long userId, java.util.List<Long> branchIds, Long homeBranchId) {
        userBranchAssignmentRepository.deleteByUserId(userId);
        java.util.LinkedHashSet<Long> ids = new java.util.LinkedHashSet<>();
        if (homeBranchId != null) {
            ids.add(homeBranchId);
        }
        if (branchIds != null) {
            ids.addAll(branchIds);
        }
        for (Long branchId : ids) {
            if (branchId == null) {
                continue;
            }
            UserBranchAssignment assignment = new UserBranchAssignment();
            assignment.setUserId(userId);
            assignment.setBranchId(branchId);
            assignment.setPrimary(branchId.equals(homeBranchId));
            userBranchAssignmentRepository.save(assignment);
        }
    }

    /**
     * Staff have an employee code; a CUSTOMER (a policyholder, not staff) does not. So the code is
     * required for staff but auto-generated for a customer login — a stable, unique value derived from
     * the customer id — which satisfies the NOT NULL/unique column without asking an admin to invent an
     * "employee code" for someone who isn't an employee.
     */
    private String resolveEmployeeCode(CreateUserRequest request, Role role, Long customerId) {
        if (StringUtils.hasText(request.employeeCode())) {
            return request.employeeCode().trim();
        }
        if (CUSTOMER_ROLE_CODE.equals(role.getCode())) {
            return "CUST-" + customerId;
        }
        throw new BusinessException("EMPLOYEE_CODE_REQUIRED", "Employee code is required for a staff user");
    }

    /**
     * A CUSTOMER login is worthless unless it points at a policyholder — the portal scopes every read
     * by customer_id, so a null one means an empty portal. Staff, conversely, must never carry one.
     * Enforcing both directions here keeps orphaned portal accounts from being created at all.
     */
    private Long resolveCustomerId(CreateUserRequest request, Role role) {
        boolean isCustomerRole = CUSTOMER_ROLE_CODE.equals(role.getCode());
        if (!isCustomerRole) {
            if (request.customerId() != null) {
                throw new BusinessException("CUSTOMER_ID_NOT_ALLOWED",
                        "Only a CUSTOMER user can be linked to a customer");
            }
            return null;
        }
        if (request.customerId() == null) {
            throw new BusinessException("CUSTOMER_ID_REQUIRED",
                    "A CUSTOMER user must be linked to a customer");
        }
        // Tenant-scoped lookup: you cannot link a login to another tenant's customer.
        customerRepository.findByIdAndIsDeletedFalse(request.customerId())
                .orElseThrow(() -> new NotFoundException("CUSTOMER_NOT_FOUND", "Customer not found"));
        return request.customerId();
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('USER_WRITE')")
    public UserResponse update(Long userId, UpdateUserRequest request) {
        AppUser user = appUserRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
        Role role = requireRole(request.roleCode());

        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhone(request.phone());
        user.setRoleId(role.getId());
        user.setStatus(parseStatus(request.status()));
        boolean isStaff = !CUSTOMER_ROLE_CODE.equals(role.getCode());
        if (isStaff) {
            user.setDepartmentId(request.departmentId());
            user.setDesignationId(request.designationId());
            user.setRegionId(request.regionId());
            user.setHomeBranchId(request.homeBranchId());
        }
        AppUser saved = appUserRepository.save(user);
        if (isStaff) {
            replaceBranchAssignments(saved.getId(), request.branchIds(), request.homeBranchId());
        }
        return toResponse(saved, roleCodeMap());
    }

    private Role requireRole(String roleCode) {
        return roleRepository.findByCodeAndIsDeletedFalse(roleCode)
                .orElseThrow(() -> new NotFoundException("ROLE_NOT_FOUND", "Role not found"));
    }

    private Map<Long, String> roleCodeMap() {
        return roleRepository.findAllByIsDeletedFalse().stream()
                .collect(Collectors.toMap(Role::getId, Role::getCode));
    }

    private static UserStatus parseStatus(String status) {
        try {
            return UserStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_STATUS", "Unknown user status: " + status);
        }
    }

    private UserResponse toResponse(AppUser u, Map<Long, String> roleCodeById) {
        String departmentName = u.getDepartmentId() == null ? null
                : departmentRepository.findByIdAndIsDeletedFalse(u.getDepartmentId())
                        .map(Department::getName).orElse(null);
        String designationName = u.getDesignationId() == null ? null
                : designationRepository.findByIdAndIsDeletedFalse(u.getDesignationId())
                        .map(Designation::getName).orElse(null);
        String regionName = u.getRegionId() == null ? null
                : regionRepository.findByIdAndIsDeletedFalse(u.getRegionId())
                        .map(Region::getName).orElse(null);
        String homeBranchName = u.getHomeBranchId() == null ? null
                : branchRepository.findByIdAndIsDeletedFalse(u.getHomeBranchId())
                        .map(Branch::getName).orElse(null);
        List<UserResponse.BranchRef> branches = userBranchAssignmentRepository
                .findAllByUserId(u.getId()).stream()
                .map(a -> {
                    String name = branchRepository.findByIdAndIsDeletedFalse(a.getBranchId())
                            .map(Branch::getName).orElse(null);
                    return new UserResponse.BranchRef(a.getBranchId(), name, a.isPrimary());
                })
                .filter(b -> b.name() != null)
                .toList();
        return new UserResponse(
                u.getId(),
                u.getEmployeeCode(),
                u.getFirstName(),
                u.getLastName(),
                u.getEmail(),
                u.getPhone(),
                u.getRoleId(),
                roleCodeById.get(u.getRoleId()),
                u.getStatus().name(),
                u.getDepartmentId(), departmentName,
                u.getDesignationId(), designationName,
                u.getRegionId(), regionName,
                u.getHomeBranchId(), homeBranchName,
                branches);
    }
}
