package com.niyotechnologies.claimlens.user.service.impl;

import com.niyotechnologies.claimlens.common.exception.BusinessException;
import com.niyotechnologies.claimlens.common.exception.NotFoundException;
import com.niyotechnologies.claimlens.role.entity.Role;
import com.niyotechnologies.claimlens.role.repository.RoleRepository;
import com.niyotechnologies.claimlens.user.dto.CreateUserRequest;
import com.niyotechnologies.claimlens.user.dto.UpdateUserRequest;
import com.niyotechnologies.claimlens.user.dto.UserResponse;
import com.niyotechnologies.claimlens.user.entity.AppUser;
import com.niyotechnologies.claimlens.user.enums.AuthProvider;
import com.niyotechnologies.claimlens.user.enums.UserStatus;
import com.niyotechnologies.claimlens.user.repository.AppUserRepository;
import com.niyotechnologies.claimlens.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    @Autowired
    private final AppUserRepository appUserRepository;
    @Autowired
    private final RoleRepository roleRepository;
    @Autowired
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('USER_READ')")
    public List<UserResponse> list(String roleCode) {
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
                .map(toResponse(roleCodeById))
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

        AppUser user = new AppUser();
        user.setEmail(request.email());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmployeeCode(request.employeeCode());
        user.setPhone(request.phone());
        user.setRoleId(role.getId());
        user.setAuthProvider(AuthProvider.LOCAL);
        if (StringUtils.hasText(request.password())) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
            user.setStatus(UserStatus.ACTIVE);
        } else {
            user.setStatus(UserStatus.INVITED);
        }
        // tenant_id is stamped automatically by @TenantId on insert.
        return toResponse(roleCodeMap()).apply(appUserRepository.save(user));
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
        return toResponse(roleCodeMap()).apply(appUserRepository.save(user));
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

    private static Function<AppUser, UserResponse> toResponse(Map<Long, String> roleCodeById) {
        return u -> new UserResponse(
                u.getId(),
                u.getEmployeeCode(),
                u.getFirstName(),
                u.getLastName(),
                u.getEmail(),
                u.getPhone(),
                u.getRoleId(),
                roleCodeById.get(u.getRoleId()),
                u.getStatus().name());
    }
}
