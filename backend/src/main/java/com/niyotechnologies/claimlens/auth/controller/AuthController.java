package com.niyotechnologies.claimlens.auth.controller;

import com.niyotechnologies.claimlens.auth.dto.GoogleLoginRequest;
import com.niyotechnologies.claimlens.auth.dto.LoginRequest;
import com.niyotechnologies.claimlens.auth.dto.LoginResponse;
import com.niyotechnologies.claimlens.auth.dto.MeResponse;
import com.niyotechnologies.claimlens.auth.dto.RefreshTokenRequest;
import com.niyotechnologies.claimlens.auth.service.AuthService;
import com.niyotechnologies.claimlens.common.response.ApiResponse;
import com.niyotechnologies.claimlens.security.model.ClaimLensPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication endpoints. Permitted without a token (see SecurityConfig).
 */
@RestController
@RequestMapping("${claimlens.api.base-path}/auth")
@RequiredArgsConstructor
public class AuthController {

    @Autowired
    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/google")
    public ApiResponse<LoginResponse> google(@Valid @RequestBody GoogleLoginRequest request) {
        return ApiResponse.success(authService.loginWithGoogle(request));
    }

    @PostMapping("/refresh-token")
    public ApiResponse<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(authService.refresh(request));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
    }

    @GetMapping("/me")
    public ApiResponse<MeResponse> me(@AuthenticationPrincipal ClaimLensPrincipal principal) {
        return ApiResponse.success(authService.me(principal));
    }
}
