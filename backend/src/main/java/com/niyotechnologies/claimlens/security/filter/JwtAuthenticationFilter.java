package com.niyotechnologies.claimlens.security.filter;

import com.niyotechnologies.claimlens.security.model.ClaimLensPrincipal;
import com.niyotechnologies.claimlens.security.service.JwtService;
import com.niyotechnologies.claimlens.security.service.PermissionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Parses a Bearer token into the SecurityContext. Stateless: no DB lookup. An absent or
 * invalid token simply leaves the request unauthenticated — the entry point returns 401
 * when the endpoint requires authentication.
 *
 * Not a @Component: instantiated in SecurityConfig so Spring Boot does not also auto-register
 * it on the raw servlet chain outside Spring Security.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final PermissionService permissionService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader(HEADER);
        if (header != null && header.startsWith(PREFIX)) {
            try {
                ClaimLensPrincipal principal = jwtService.parse(header.substring(PREFIX.length()));
                List<SimpleGrantedAuthority> authorities =
                        permissionService.permissionCodesForRole(principal.tenantId(), principal.roleId()).stream()
                                .map(SimpleGrantedAuthority::new)
                                .toList();
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception ex) {
                // Invalid/expired/forged token: leave unauthenticated. Do not leak why.
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
