package com.niyotechnologies.claimlens.user.controller;

import com.niyotechnologies.claimlens.common.response.ApiResponse;
import com.niyotechnologies.claimlens.security.model.ClaimLensPrincipal;
import com.niyotechnologies.claimlens.user.dto.ChangePasswordRequest;
import com.niyotechnologies.claimlens.user.dto.ProfileResponse;
import com.niyotechnologies.claimlens.user.dto.UpdateProfileRequest;
import com.niyotechnologies.claimlens.user.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * The signed-in user's own profile. Every operation is scoped to the caller's own record via the
 * principal — no USER_* admin permission required, so any role (staff or customer) can manage their
 * own details and password.
 */
@RestController
@RequestMapping("${claimlens.api.base-path}/profile")
@RequiredArgsConstructor
public class ProfileController {

    @Autowired
    private final ProfileService profileService;

    @GetMapping
    public ApiResponse<ProfileResponse> me(@AuthenticationPrincipal ClaimLensPrincipal principal) {
        return ApiResponse.success(profileService.getProfile(principal.userId()));
    }

    @PutMapping
    public ApiResponse<ProfileResponse> update(
            @AuthenticationPrincipal ClaimLensPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success(profileService.updateProfile(principal.userId(), request));
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @AuthenticationPrincipal ClaimLensPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        profileService.changePassword(principal.userId(), request);
    }
}
