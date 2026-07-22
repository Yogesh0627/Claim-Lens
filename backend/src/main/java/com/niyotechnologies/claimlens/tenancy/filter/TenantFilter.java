package com.niyotechnologies.claimlens.tenancy.filter;

import com.niyotechnologies.claimlens.security.model.ClaimLensPrincipal;
import com.niyotechnologies.claimlens.tenancy.context.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Binds the tenant to the current thread from the authenticated principal, and always clears
 * it in a finally block — Tomcat reuses threads, so a leaked tenant would bleed into the next
 * request. Runs after JwtAuthenticationFilter, since the tenant comes from the principal.
 *
 * Not a @Component: instantiated in SecurityConfig so it is not auto-registered on the raw
 * servlet chain (where it would run before authentication and see no principal).
 */
public class TenantFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null
                    && authentication.getPrincipal() instanceof ClaimLensPrincipal principal) {
                TenantContext.set(principal.tenantId());
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
