package com.cloudpilot.backend.auth;

import com.cloudpilot.backend.tenants.Tenant;
import com.cloudpilot.backend.tenants.TenantRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final TenantRepository tenantRepository;

    public AuthController(
            AuthService authService,
            TenantRepository tenantRepository) {
        this.authService = authService;
        this.tenantRepository = tenantRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        UserResponse user = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(user);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        LoginResponse response = authService.login(request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {

        Tenant tenant = tenantRepository.findById(authenticatedUser.tenantId())
            .orElseThrow(() -> new IllegalStateException("Tenant not found"));

        return ResponseEntity.ok(
            UserResponse.from(authenticatedUser.user(), tenant)
        );
    }

    @GetMapping("/context")
    public ResponseEntity<AuthContextResponse> getAuthenticationContext(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {

        AuthContextResponse response = new AuthContextResponse(
                authenticatedUser.user().getId(),
                authenticatedUser.user().getEmail(),
                authenticatedUser.tenantId(),
                authenticatedUser.role(),
                authenticatedUser.permissions()
        );

        return ResponseEntity.ok(response);
    }
}
