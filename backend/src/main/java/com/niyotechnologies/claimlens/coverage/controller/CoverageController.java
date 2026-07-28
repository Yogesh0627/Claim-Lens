package com.niyotechnologies.claimlens.coverage.controller;

import com.niyotechnologies.claimlens.common.response.ApiResponse;
import com.niyotechnologies.claimlens.coverage.dto.AskCoverageRequest;
import com.niyotechnologies.claimlens.coverage.dto.AskCoverageResponse;
import com.niyotechnologies.claimlens.coverage.dto.AskableProductResponse;
import com.niyotechnologies.claimlens.coverage.service.CoverageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${claimlens.api.base-path}/coverage")
@RequiredArgsConstructor
public class CoverageController {

    @Autowired
    private final CoverageService coverageService;

    @PostMapping("/ask")
    public ApiResponse<AskCoverageResponse> ask(@Valid @RequestBody AskCoverageRequest request) {
        return ApiResponse.success(coverageService.ask(request));
    }

    /** Products with ingested wording — populates the assistant's "which product?" picker. */
    @GetMapping("/products")
    public ApiResponse<List<AskableProductResponse>> askableProducts() {
        return ApiResponse.success(coverageService.askableProducts());
    }
}
