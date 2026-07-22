package com.niyotechnologies.claimlens.analytics.service;

import com.niyotechnologies.claimlens.analytics.dto.DashboardResponse;
import com.niyotechnologies.claimlens.claim.repository.ClaimRepository;
import com.niyotechnologies.claimlens.fraud.repository.FraudScoreRepository;
import com.niyotechnologies.claimlens.tenancy.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Live dashboard aggregates (no pre-computed snapshots at V1 scale). */
@Service
@RequiredArgsConstructor
public class DashboardService {

    @Autowired
    private final ClaimRepository claimRepository;
    @Autowired
    private final FraudScoreRepository fraudScoreRepository;

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ANALYTICS_READ')")
    public DashboardResponse getDashboard() {
        Map<String, Long> claimsByStatus = toMap(claimRepository.countGroupedByStatus());
        Map<String, Long> fraudRiskDistribution =
                toMap(fraudScoreRepository.countGroupedByRiskLevel(TenantContext.getTenantId()));
        return new DashboardResponse(claimsByStatus, fraudRiskDistribution);
    }

    private Map<String, Long> toMap(List<Object[]> rows) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            result.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }
        return result;
    }
}
