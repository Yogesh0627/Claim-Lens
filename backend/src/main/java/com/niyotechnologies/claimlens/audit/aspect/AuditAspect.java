package com.niyotechnologies.claimlens.audit.aspect;

import com.niyotechnologies.claimlens.audit.annotation.Auditable;
import com.niyotechnologies.claimlens.audit.service.AuditService;
import com.niyotechnologies.claimlens.security.model.ClaimLensPrincipal;
import com.niyotechnologies.claimlens.tenancy.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * "Auditability by default": records an audit entry after any @Auditable method returns successfully.
 * Runs after the method body, so a method that throws writes no audit. Entity id = the first Long arg
 * (by convention the id). User comes from the security context, tenant from TenantContext.
 */
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    @Autowired
    private final AuditService auditService;

    @AfterReturning("@annotation(auditable)")
    public void recordAudit(JoinPoint joinPoint, Auditable auditable) {
        Long tenantId = TenantContext.getTenantIdOrNull();
        if (tenantId == null) {
            return; // unauthenticated / no tenant bound — nothing meaningful to attribute
        }
        auditService.record(
                auditable.action(),
                auditable.entityType(),
                firstLongArgument(joinPoint.getArgs()),
                currentUserId(),
                tenantId);
    }

    private Long firstLongArgument(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof Long value) {
                return value;
            }
        }
        return null;
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof ClaimLensPrincipal principal) {
            return principal.userId();
        }
        return null;
    }
}
