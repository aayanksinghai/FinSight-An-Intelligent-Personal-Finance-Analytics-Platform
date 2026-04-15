package com.finsight.ingestion.api.admin;

import com.finsight.ingestion.api.IngestionJobResponse;
import com.finsight.ingestion.persistence.IngestionJobDocument;
import com.finsight.ingestion.service.IngestionJobService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/ingestion")
public class AdminIngestionJobController {

    private final IngestionJobService ingestionJobService;

    public AdminIngestionJobController(IngestionJobService ingestionJobService) {
        this.ingestionJobService = ingestionJobService;
    }

    @GetMapping("/jobs")
    public ResponseEntity<Page<IngestionJobResponse>> listAllJobs(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireAdmin(jwt);
        Page<IngestionJobDocument> jobPage = ingestionJobService.listAllJobs(page, size);
        return ResponseEntity.ok(jobPage.map(IngestionJobResponse::from));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(@AuthenticationPrincipal Jwt jwt) {
        requireAdmin(jwt);
        return ResponseEntity.ok(ingestionJobService.getJobStats());
    }

    private void requireAdmin(Jwt jwt) {
        if (jwt == null || !"ADMIN".equalsIgnoreCase(jwt.getClaimAsString("role"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role required");
        }
    }
}
