package com.niyotechnologies.claimlens.user.service;

import com.niyotechnologies.claimlens.user.dto.CreateUserRequest;
import com.niyotechnologies.claimlens.user.dto.UpdateUserRequest;
import com.niyotechnologies.claimlens.user.dto.UserResponse;
import com.niyotechnologies.claimlens.common.response.PagedResponse;

import java.util.List;

public interface UserService {

    /** Paged directory listing for the Users screen (all roles). */
    PagedResponse<UserResponse> list(int page, int size);

    /** All users (unpaged), optionally filtered to one role code (e.g. INVESTIGATOR) — for pickers. */
    List<UserResponse> options(String roleCode);

    UserResponse create(CreateUserRequest request);

    UserResponse update(Long userId, UpdateUserRequest request);
}
