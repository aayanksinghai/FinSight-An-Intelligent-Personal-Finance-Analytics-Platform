package com.finsight.ingestion.api;

import com.finsight.ingestion.persistence.IngestionJobDocument;
import com.finsight.ingestion.service.IngestionJobService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for bank statement ingestion.
 *
 * <ul>
 *   <li>POST /api/ingestion/upload    — upload a statement file (202 Accepted)</li>
 *   <li>GET  /api/ingestion/jobs      — list caller's jobs (paginated)</li>
 *   <li>GET  /api/ingestion/jobs/{id} — get a single job's status</li>
 * </ul>
 *
 * All endpoints require a valid JWT (enforced by SecurityConfig).
 * The ownerEmail is extracted from the JWT {@code sub} claim.
 */
@RestController
@RequestMapping("/api/ingestion")
public class IngestionJobController {

    private final IngestionJobService ingestionJobService;

    public IngestionJobController(IngestionJobService ingestionJobService) {
        this.ingestionJobService = ingestionJobService;
    }

    /**
     * Upload a bank statement file.
     * Returns 202 Accepted immediately with the job ID.
     * Parsing happens asynchronously in the background.
     */
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<IngestionJobResponse> upload(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("file") MultipartFile file) {

        String ownerEmail = jwt.getSubject();
        IngestionJobDocument job = ingestionJobService.createJob(ownerEmail, file);
        return ResponseEntity
            .status(HttpStatus.ACCEPTED)
            .body(IngestionJobResponse.from(job));
    }

    /**
     * List all ingestion jobs for the authenticated user.
     */
    @GetMapping("/jobs")
    public ResponseEntity<Page<IngestionJobResponse>> listJobs(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        String ownerEmail = jwt.getSubject();
        Page<IngestionJobDocument> jobPage = ingestionJobService.listJobs(ownerEmail, page, size);
        Page<IngestionJobResponse> response = jobPage.map(IngestionJobResponse::from);
        return ResponseEntity.ok(response);
    }

    /**
     * Get a specific ingestion job.
     * Returns 404 if the job does not exist or belongs to a different user.
     */
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<IngestionJobResponse> getJob(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String jobId) {

        String ownerEmail = jwt.getSubject();
        IngestionJobDocument job = ingestionJobService.getJob(ownerEmail, jobId);
        return ResponseEntity.ok(IngestionJobResponse.from(job));
    }
}
