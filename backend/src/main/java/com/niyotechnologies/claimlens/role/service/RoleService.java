package com.niyotechnologies.claimlens.role.service;

import com.niyotechnologies.claimlens.role.dto.response.RoleResponse;

import java.util.List;

public interface RoleService {

    List<RoleResponse> getAllRoles();

    RoleResponse getRole(Long roleId);
}