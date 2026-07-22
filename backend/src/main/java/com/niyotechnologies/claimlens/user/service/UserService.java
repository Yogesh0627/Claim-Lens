package com.niyotechnologies.claimlens.user.service;

import com.niyotechnologies.claimlens.user.dto.CreateUserRequest;
import com.niyotechnologies.claimlens.user.dto.UpdateUserRequest;
import com.niyotechnologies.claimlens.user.dto.UserResponse;

import java.util.List;

public interface UserService {

    /** Lists tenant users, optionally filtered to a single role code (e.g. INVESTIGATOR). */
    List<UserResponse> list(String roleCode);

    UserResponse create(CreateUserRequest request);

    UserResponse update(Long userId, UpdateUserRequest request);
}
