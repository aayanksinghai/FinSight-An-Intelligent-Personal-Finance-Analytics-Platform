package com.finsight.admin.api;

import com.finsight.admin.persistence.AdminConfig;
import com.finsight.admin.persistence.AdminConfigRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/config")
public class AdminConfigController {

    private final AdminConfigRepository adminConfigRepository;

    public AdminConfigController(AdminConfigRepository adminConfigRepository) {
        this.adminConfigRepository = adminConfigRepository;
    }

    @GetMapping
    public ResponseEntity<Map<String, String>> getAllConfigs(@AuthenticationPrincipal Jwt jwt) {
        requireAdmin(jwt);
        List<AdminConfig> configs = adminConfigRepository.findAll();
        Map<String, String> configMap = configs.stream()
            .collect(Collectors.toMap(AdminConfig::getConfigKey, AdminConfig::getConfigValue));
        return ResponseEntity.ok(configMap);
    }

    @PostMapping
    public ResponseEntity<Void> updateConfig(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, String> updates) {
        requireAdmin(jwt);
        updates.forEach((key, value) -> {
            AdminConfig config = adminConfigRepository.findById(key).orElse(new AdminConfig());
            config.setConfigKey(key);
            config.setConfigValue(value);
            // Default description if missing, or retain exist
            if (config.getDescription() == null) {
                config.setDescription("Custom updated threshold for " + key);
            }
            adminConfigRepository.save(config);
        });
        return ResponseEntity.ok().build();
    }

    private void requireAdmin(Jwt jwt) {
        if (jwt == null || !"ADMIN".equalsIgnoreCase(jwt.getClaimAsString("role"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role required");
        }
    }
}
