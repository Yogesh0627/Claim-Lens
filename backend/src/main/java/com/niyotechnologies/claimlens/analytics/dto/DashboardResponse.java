package com.niyotechnologies.claimlens.analytics.dto;

import java.util.Map;

public record DashboardResponse(
        Map<String, Long> claimsByStatus,
        Map<String, Long> fraudRiskDistribution
) {
}
