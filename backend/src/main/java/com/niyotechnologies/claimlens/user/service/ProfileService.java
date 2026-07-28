package com.niyotechnologies.claimlens.user.service;

import com.niyotechnologies.claimlens.common.exception.BusinessException;
import com.niyotechnologies.claimlens.common.exception.NotFoundException;
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
import com.niyotechnologies.claimlens.user.dto.ChangePasswordRequest;
import com.niyotechnologies.claimlens.user.dto.ProfileResponse;
import com.niyotechnologies.claimlens.user.dto.UpdateProfileRequest;
import com.niyotechnologies.claimlens.user.entity.AppUser;
import com.niyotechnologies.claimlens.user.repository.AppUserRepository;
import com.niyotechnologies.claimlens.user.repository.UserBranchAssignmentRepository;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Self-service profile operations — every method acts on the authenticated user's OWN record (by the
 * userId from the principal), so any role can use it without USER_* admin permissions.
 */
@Service
@RequiredArgsConstructor
public class ProfileService {

    @Autowired
    private final AppUserRepository appUserRepository;
    @Autowired
    private final RoleRepository roleRepository;
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
    @Autowired
    private final CustomerRepository customerRepository;
    @Autowired
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ProfileResponse getProfile(Long userId) {
        return toResponse(requireUser(userId));
    }

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public ProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        AppUser user = requireUser(userId);
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhone(request.phone());
        return toResponse(appUserRepository.save(user));
    }

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public void changePassword(Long userId, ChangePasswordRequest request) {
        AppUser user = requireUser(userId);
        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException("INCORRECT_PASSWORD", "Your current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        appUserRepository.save(user);
    }

    private AppUser requireUser(Long userId) {
        return appUserRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
    }

    private ProfileResponse toResponse(AppUser u) {
        Role role = u.getRoleId() == null ? null
                : roleRepository.findByIdAndIsDeletedFalse(u.getRoleId()).orElse(null);
        String departmentName = u.getDepartmentId() == null ? null
                : departmentRepository.findByIdAndIsDeletedFalse(u.getDepartmentId())
                        .map(Department::getName).orElse(null);
        String designationName = u.getDesignationId() == null ? null
                : designationRepository.findByIdAndIsDeletedFalse(u.getDesignationId())
                        .map(Designation::getName).orElse(null);
        String regionName = u.getRegionId() == null ? null
                : regionRepository.findByIdAndIsDeletedFalse(u.getRegionId())
                        .map(Region::getName).orElse(null);
        Branch homeBranch = u.getHomeBranchId() == null ? null
                : branchRepository.findByIdAndIsDeletedFalse(u.getHomeBranchId()).orElse(null);
        String homeBranchName = homeBranch == null ? null : homeBranch.getName();
        String homeBranchLocation = homeBranch == null ? null : location(homeBranch);
        // All assigned branches, home first.
        List<String> branchNames = userBranchAssignmentRepository.findAllByUserId(u.getId()).stream()
                .sorted((a, b) -> Boolean.compare(b.isPrimary(), a.isPrimary()))
                .map(a -> branchRepository.findByIdAndIsDeletedFalse(a.getBranchId())
                        .map(Branch::getName).orElse(null))
                .filter(n -> n != null)
                .toList();
        String customerNumber = u.getCustomerId() == null ? null
                : customerRepository.findByIdAndIsDeletedFalse(u.getCustomerId())
                        .map(c -> c.getCustomerNumber()).orElse(null);
        return new ProfileResponse(
                u.getId(), u.getFirstName(), u.getLastName(), u.getEmail(), u.getPhone(),
                u.getEmployeeCode(),
                role == null ? null : role.getCode(),
                role == null ? null : role.getName(),
                departmentName, designationName, regionName,
                homeBranchName, homeBranchLocation, branchNames, customerNumber);
    }

    /** "New Delhi, Delhi" from a branch's city/state — either part may be blank. */
    private static String location(Branch b) {
        String city = b.getCity() == null ? "" : b.getCity().trim();
        String state = b.getState() == null ? "" : b.getState().trim();
        String loc = (city + (city.isEmpty() || state.isEmpty() ? "" : ", ") + state).trim();
        return loc.isEmpty() ? null : loc;
    }
}
