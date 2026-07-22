package com.niyotechnologies.claimlens.audit.controller;

import com.niyotechnologies.claimlens.audit.dto.AuditEntryResponse;
import com.niyotechnologies.claimlens.audit.service.AuditService;
import com.niyotechnologies.claimlens.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${claimlens.api.base-path}/claims/{claimId}/audit")
@RequiredArgsConstructor
public class AuditController {

    @Autowired
    private final AuditService auditService;

    @GetMapping
    public ApiResponse<List<AuditEntryResponse>> claimAudit(@PathVariable Long claimId) {
        return ApiResponse.success(auditService.getClaimAudit(claimId));
    }
}
