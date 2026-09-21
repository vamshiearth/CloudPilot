package com.cloudpilot.backend.auth;

import com.cloudpilot.backend.users.UserRepository;
import com.cloudpilot.backend.rbac.Permission;
import com.cloudpilot.backend.tenants.TenantMembership;
import com.cloudpilot.backend.tenants.TenantMembershipRepository;
import com.cloudpilot.backend.telemetry.TenantTelemetry;
import com.cloudpilot.backend.observability.TenantWorkloadTracker;
import io.jsonwebtoken.JwtException;
import io.opentelemetry.context.Scope;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@SuppressWarnings("null")
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
        private final TenantMembershipRepository membershipRepository;
        private final TenantWorkloadTracker tenantWorkloadTracker;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository,
            TenantMembershipRepository membershipRepository,
            TenantWorkloadTracker tenantWorkloadTracker) {

        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.tenantWorkloadTracker = tenantWorkloadTracker;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader =
                request.getHeader("Authorization");

        if (authHeader == null ||
                !authHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {

            String email =
                    jwtService.extractEmail(token);
            Long tenantId = jwtService.extractTenantId(token);

            if (email != null &&
                    SecurityContextHolder
                            .getContext()
                            .getAuthentication() == null) {

                userRepository.findByEmail(email)
                        .ifPresent(user -> {
                            if (!jwtService.isTokenValid(token, user)) {
                                return;
                            }

                            TenantMembership membership = membershipRepository
                                    .findByUserIdAndTenantIdAndStatus(
                                            user.getId(),
                                            tenantId,
                                            "ACTIVE"
                                    )
                                    .orElseThrow(() ->
                                            new IllegalArgumentException(
                                                    "Active tenant membership not found"
                                            )
                                    );

                            if (membership.getRole() == null) {
                                throw new IllegalArgumentException(
                                        "Active tenant membership has no role"
                                );
                            }

                            String roleName = membership.getRole().getName();
                            Set<String> permissions = membership.getRole()
                                    .getPermissions()
                                    .stream()
                                    .map(Permission::getName)
                                    .collect(Collectors.toUnmodifiableSet());

                            AuthenticatedUser principal =
                                    new AuthenticatedUser(
                                            user,
                                            tenantId,
                                            roleName,
                                            permissions
                                    );

                            List<GrantedAuthority> authorities =
                                    new ArrayList<>();
                            authorities.add(new SimpleGrantedAuthority(
                                    "ROLE_" + roleName
                            ));

                            for (String permission : permissions) {
                                authorities.add(new SimpleGrantedAuthority(
                                        permission
                                ));
                            }

                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(
                                            principal,
                                            null,
                                            authorities
                                    );

                            authentication.setDetails(
                                    new WebAuthenticationDetailsSource()
                                            .buildDetails(request)
                            );

                            SecurityContextHolder
                                    .getContext()
                                    .setAuthentication(authentication);
                        });
            }

        } catch (JwtException | IllegalArgumentException ignored) {
            // Invalid JWT: user remains unauthenticated.
        }

        if (SecurityContextHolder.getContext().getAuthentication() == null
                || !(SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal() instanceof AuthenticatedUser authenticatedUser)) {
            filterChain.doFilter(request, response);
            return;
        }

        long requestStartNanos = System.nanoTime();
        boolean requestFailed = false;

        try (Scope tenantTelemetryScope =
                     TenantTelemetry.bindTenant(authenticatedUser.tenantId())) {
            try {
                filterChain.doFilter(request, response);
            } catch (IOException | ServletException | RuntimeException exception) {
                requestFailed = true;
                throw exception;
            } finally {
                if (shouldMeasureTenantWorkload(request)) {
                    int statusCode = response.getStatus();
                    if (requestFailed && statusCode < 400) {
                        statusCode = 500;
                    }
                    tenantWorkloadTracker.record(
                            authenticatedUser.tenantId(),
                            System.nanoTime() - requestStartNanos,
                            statusCode
                    );
                }
            }
        }
    }

    private boolean shouldMeasureTenantWorkload(HttpServletRequest request) {
                String path = request.getRequestURI();
                return !path.startsWith("/api/tenant/workload")
                                && !path.startsWith("/api/admin/observability");
    }
}