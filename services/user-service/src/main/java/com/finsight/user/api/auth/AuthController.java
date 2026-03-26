package com.finsight.user.api.auth;

import com.finsight.user.security.DevAuthService;
import com.finsight.user.security.JwtTokenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/users/auth")
public class AuthController {

    private final DevAuthService devAuthService;
    private final JwtTokenService jwtTokenService;

    public AuthController(DevAuthService devAuthService, JwtTokenService jwtTokenService) {
        this.devAuthService = devAuthService;
        this.jwtTokenService = jwtTokenService;
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
        if (!devAuthService.authenticate(request.email(), request.password())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        String accessToken = jwtTokenService.issueToken(request.email());
        return ResponseEntity.ok(new AuthTokenResponse(accessToken, "Bearer", jwtTokenService.getExpirySeconds()));
    }
}
