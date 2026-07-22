package com.niyotechnologies.claimlens.analytics.controller;

import com.niyotechnologies.claimlens.analytics.dto.DashboardResponse;
import com.niyotechnologies.claimlens.analytics.service.DashboardService;
import com.niyotechnologies.claimlens.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${claimlens.api.base-path}/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    @Autowired
    private final DashboardService dashboardService;

    @GetMapping("/dashboard")
    public ApiResponse<DashboardResponse> dashboard() {
        return ApiResponse.success(dashboardService.getDashboard());
    }
}
