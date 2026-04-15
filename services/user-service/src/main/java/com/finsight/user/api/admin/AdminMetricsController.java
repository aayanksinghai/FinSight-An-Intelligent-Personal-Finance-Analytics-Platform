package com.finsight.user.api.admin;

import com.finsight.user.admin.AdminUserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/metrics")
public class AdminMetricsController {

    private final AdminUserService adminUserService;

    public AdminMetricsController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping("/users")
    public ResponseEntity<Map<String, Long>> getUserMetrics(@AuthenticationPrincipal Jwt jwt) {
        requireAdmin(jwt);
        long totalUsers = adminUserService.getTotalUsers();
        return ResponseEntity.ok(Map.of("totalUsers", totalUsers));
    }

    private void requireAdmin(Jwt jwt) {
        if (jwt == null || !"ADMIN".equalsIgnoreCase(jwt.getClaimAsString("role"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role required");
        }
    }
}
