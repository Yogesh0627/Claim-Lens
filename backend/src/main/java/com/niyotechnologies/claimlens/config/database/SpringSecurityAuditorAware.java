package com.niyotechnologies.claimlens.config.database;

import com.niyotechnologies.claimlens.security.model.ClaimLensPrincipal;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Supplies the current user id for @CreatedBy/@LastModifiedBy. Returns empty when there is no
 * authenticated principal (startup, background jobs, unauthenticated calls) — we do not invent
 * a fake system user id.
 */
@Component("auditorAware")
public class SpringSecurityAuditorAware implements AuditorAware<Long> {

    @Override
    public Optional<Long> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.getPrincipal() instanceof ClaimLensPrincipal principal) {
            return Optional.ofNullable(principal.userId());
        }
        return Optional.empty();
    }
}
