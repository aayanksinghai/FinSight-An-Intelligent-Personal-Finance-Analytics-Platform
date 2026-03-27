package com.finsight.user.api.admin;

import com.finsight.user.admin.AdminUserService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ResponseEntity<Page<AdminUserResponse>> listUsers(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireAdmin(jwt);
        return ResponseEntity.ok(adminUserService.listUsers(status, page, size));
    }

    @PatchMapping("/{email}/deactivate")
    public ResponseEntity<Void> deactivate(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String email) {
        requireAdmin(jwt);
        adminUserService.deactivateUser(email);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{email}/activate")
    public ResponseEntity<Void> activate(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String email) {
        requireAdmin(jwt);
        adminUserService.activateUser(email);
        return ResponseEntity.noContent().build();
    }

    private void requireAdmin(Jwt jwt) {
        if (jwt == null || !"ADMIN".equalsIgnoreCase(jwt.getClaimAsString("role"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role required");
        }
    }
}

