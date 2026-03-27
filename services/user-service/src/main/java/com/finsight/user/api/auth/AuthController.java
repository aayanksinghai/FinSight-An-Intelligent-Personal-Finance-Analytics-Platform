package com.finsight.user.api.auth;

import com.finsight.user.security.AuthSessionService;
import com.finsight.user.security.DevAuthService;
import com.finsight.user.security.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/users/auth")
public class AuthController {

    private final DevAuthService devAuthService;
    private final AuthSessionService authSessionService;
    private final PasswordResetService passwordResetService;

    public AuthController(
            DevAuthService devAuthService,
            AuthSessionService authSessionService,
            PasswordResetService passwordResetService) {
        this.devAuthService = devAuthService;
        this.authSessionService = authSessionService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        boolean created = devAuthService.register(request.email(), request.password());
        if (!created) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RegisterResponse(request.email(), "User registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthTokenResponse> login(@Valid @RequestBody LoginRequest request) {
        DevAuthService.AuthenticatedUser user = devAuthService.authenticateUser(request.email(), request.password());
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        return ResponseEntity.ok(authSessionService.issueSession(user.email(), user.role()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authSessionService.refreshSession(request.refreshToken()));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangePasswordRequest request) {
        devAuthService.changePassword(jwt.getSubject(), request.currentPassword(), request.newPassword());
        authSessionService.logoutAllSessions(jwt);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt jwt) {
        authSessionService.logout(jwt);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(@AuthenticationPrincipal Jwt jwt) {
        authSessionService.logoutAllSessions(jwt);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<PasswordResetResponse> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        return ResponseEntity.ok(passwordResetService.requestPasswordReset(request.email()));
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        passwordResetService.confirmPasswordReset(request.resetToken(), request.newPassword());
        return ResponseEntity.noContent().build();
    }
}
