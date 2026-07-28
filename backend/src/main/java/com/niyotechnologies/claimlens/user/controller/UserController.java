package com.niyotechnologies.claimlens.user.controller;

import com.niyotechnologies.claimlens.common.response.ApiResponse;
import com.niyotechnologies.claimlens.common.response.PagedResponse;
import com.niyotechnologies.claimlens.user.dto.CreateUserRequest;
import com.niyotechnologies.claimlens.user.dto.UpdateUserRequest;
import com.niyotechnologies.claimlens.user.dto.UserResponse;
import com.niyotechnologies.claimlens.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${claimlens.api.base-path}/users")
@RequiredArgsConstructor
public class UserController {

    @Autowired
    private final UserService userService;

    @GetMapping
    public ApiResponse<PagedResponse<UserResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(userService.list(page, size));
    }

    /** Unpaged — for staff pickers (e.g. the investigator dropdown), optionally filtered by role. */
    @GetMapping("/options")
    public ApiResponse<List<UserResponse>> options(
            @RequestParam(name = "role", required = false) String roleCode) {
        return ApiResponse.success(userService.options(roleCode));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.success(userService.create(request));
    }

    @PutMapping("/{userId}")
    public ApiResponse<UserResponse> update(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.success(userService.update(userId, request));
    }
}
