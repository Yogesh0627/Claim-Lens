package com.niyotechnologies.claimlens.auth.service;

import com.niyotechnologies.claimlens.auth.dto.GoogleLoginRequest;
import com.niyotechnologies.claimlens.auth.dto.LoginRequest;
import com.niyotechnologies.claimlens.auth.dto.LoginResponse;
import com.niyotechnologies.claimlens.auth.dto.MeResponse;
import com.niyotechnologies.claimlens.auth.dto.RefreshTokenRequest;
import com.niyotechnologies.claimlens.security.model.ClaimLensPrincipal;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    LoginResponse loginWithGoogle(GoogleLoginRequest request);

    LoginResponse refresh(RefreshTokenRequest request);

    void logout(RefreshTokenRequest request);

    MeResponse me(ClaimLensPrincipal principal);
}
