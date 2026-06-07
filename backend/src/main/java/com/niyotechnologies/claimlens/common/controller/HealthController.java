package com.niyotechnologies.claimlens.common.controller;

import com.niyotechnologies.claimlens.common.exception.NotFoundException;
import com.niyotechnologies.claimlens.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/")
    public ApiResponse<String> health() {

        return ApiResponse.success(
                "ClaimLens API Running"
        );
    }

    @GetMapping("/error-test")
    public String errorTest() {
        throw new NotFoundException(
                "USER_NOT_FOUND",
                "User not found"
        );
    }
}