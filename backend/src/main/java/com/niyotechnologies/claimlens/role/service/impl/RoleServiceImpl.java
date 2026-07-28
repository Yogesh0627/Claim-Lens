package com.niyotechnologies.claimlens.role.service.impl;

import com.niyotechnologies.claimlens.common.exception.NotFoundException;
import com.niyotechnologies.claimlens.role.dto.response.RoleResponse;
import com.niyotechnologies.claimlens.role.entity.Role;
import com.niyotechnologies.claimlens.role.enums.RoleStatus;
import com.niyotechnologies.claimlens.role.repository.RoleRepository;
import com.niyotechnologies.claimlens.role.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    @Override
    // The role table is global (not tenant-scoped). Without a guard, any authenticated principal —
    // including a portal CUSTOMER — could enumerate every role code/name. Gate on USER_READ (staff
    // user-administration), which the only legitimate caller (the user create/edit form) already holds.
    @PreAuthorize("hasAuthority('USER_READ')")
    public List<RoleResponse> getAllRoles() {
        return roleRepository
                .findAllByStatusAndIsDeletedFalse(RoleStatus.ACTIVE)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @PreAuthorize("hasAuthority('USER_READ')")
    public RoleResponse getRole(Long roleId) {
        Role role = getRoleOrThrow(roleId);

        return mapToResponse(role);
    }

    private Role getRoleOrThrow(Long roleId) {
        return roleRepository.findByIdAndIsDeletedFalse(roleId)
                .orElseThrow(() ->
                        new NotFoundException("ROLE_NOT_FOUND","Role not found with id: " + roleId));
    }

    private RoleResponse mapToResponse(Role role) {
        return RoleResponse.builder()
                .id(role.getId())
                .code(role.getCode())
                .name(role.getName())
                .description(role.getDescription())
                .status(role.getStatus())
                .isSystemRole(role.getIsSystemRole())
                .build();
    }
}