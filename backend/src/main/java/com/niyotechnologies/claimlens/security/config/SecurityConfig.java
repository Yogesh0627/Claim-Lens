package com.niyotechnologies.claimlens.security.config;

import com.niyotechnologies.claimlens.security.filter.JwtAuthenticationFilter;
import com.niyotechnologies.claimlens.security.filter.RateLimitFilter;
import com.niyotechnologies.claimlens.security.ratelimit.RateLimitProperties;
import com.niyotechnologies.claimlens.security.ratelimit.RateLimiter;
import com.niyotechnologies.claimlens.security.service.JwtService;
import com.niyotechnologies.claimlens.security.service.PermissionService;
import com.niyotechnologies.claimlens.tenancy.filter.TenantFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Stateless JWT security. Filter order is load-bearing: JwtAuthenticationFilter populates the
 * principal, then TenantFilter derives the tenant from it. Both filters are instantiated here
 * (not @Component) so Boot does not also auto-register them outside the Spring Security chain.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtService jwtService;
    private final PermissionService permissionService;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final RateLimiter rateLimiter;
    private final RateLimitProperties rateLimitProperties;

    /** Browser origins allowed to call the API (the frontend). Comma-separated; set per environment. */
    @Value("${claimlens.security.cors.allowed-origins:http://localhost:3000}")
    private List<String> allowedOrigins;

    /** Base path, so the rate limiter matches /auth/** wherever the API is mounted. */
    @Value("${claimlens.api.base-path:/api/v1}")
    private String apiBasePath;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationFilter jwtAuthenticationFilter =
                new JwtAuthenticationFilter(jwtService, permissionService);
        TenantFilter tenantFilter = new TenantFilter();
        RateLimitFilter rateLimitFilter =
                new RateLimitFilter(rateLimiter, rateLimitProperties, apiBasePath);

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                // Order is load-bearing: authenticate → throttle → resolve tenant.
                // Each line anchors to the filter added before it, because Spring Security can only
                // order a filter relative to one it has ALREADY registered.
                // Rate limiting sits after authentication so cost-bearing endpoints can be keyed per
                // user, and still ahead of every controller, so a throttled login is rejected before
                // any password hashing or database work.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(rateLimitFilter, JwtAuthenticationFilter.class)
                .addFilterAfter(tenantFilter, RateLimitFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS for the browser frontend. We authenticate with a Bearer token in the Authorization header
     * (not cookies), so credentials are not allowed; the Authorization header is. Content-Disposition
     * is exposed so document downloads can surface their filename.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Content-Disposition"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

