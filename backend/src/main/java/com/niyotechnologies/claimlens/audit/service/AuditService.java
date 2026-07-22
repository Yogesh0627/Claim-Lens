package com.niyotechnologies.claimlens.audit.service;

import com.niyotechnologies.claimlens.audit.dto.AuditEntryResponse;
import com.niyotechnologies.claimlens.audit.entity.AuditLog;
import com.niyotechnologies.claimlens.audit.repository.AuditLogRepository;
import com.niyotechnologies.claimlens.claim.repository.ClaimRepository;
import com.niyotechnologies.claimlens.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditService {

    private static final String CLAIM = "CLAIM";

    @Autowired
    private final AuditLogRepository auditLogRepository;
    @Autowired
    private final ClaimRepository claimRepository;

    /** REQUIRED: joins the business transaction when present (so a rolled-back action leaves no audit). */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRED)
    public void record(String action, String entityType, Long entityId, Long userId, Long tenantId) {
        AuditLog log = new AuditLog();
        log.setTenantId(tenantId);
        log.setUserId(userId);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('AUDIT_READ')")
    public List<AuditEntryResponse> getClaimAudit(Long claimId) {
        // Tenant-scoped verification: a claim from another tenant returns 404, not another tenant's audit.
        claimRepository.findByIdAndIsDeletedFalse(claimId)
                .orElseThrow(() -> new NotFoundException("CLAIM_NOT_FOUND", "Claim not found"));
        return auditLogRepository.findAllByEntityTypeAndEntityIdOrderByCreatedAtAsc(CLAIM, claimId).stream()
                .map(e -> new AuditEntryResponse(e.getId(), e.getAction(), e.getEntityType(),
                        e.getEntityId(), e.getUserId(), e.getCreatedAt()))
                .toList();
    }
}
