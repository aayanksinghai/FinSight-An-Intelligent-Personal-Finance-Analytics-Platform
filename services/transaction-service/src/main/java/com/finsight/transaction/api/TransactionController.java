package com.finsight.transaction.api;

import com.finsight.transaction.service.TransactionService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public ResponseEntity<Page<TransactionResponse>> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(transactionService.list(jwt.getSubject(), from, to, category, type, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        return ResponseEntity.ok(transactionService.getById(jwt.getSubject(), id));
    }

    @GetMapping("/summary")
    public ResponseEntity<List<Map<String, Object>>> getSummary(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        
        Instant end = (to != null) ? to : Instant.now();
        Instant start = (from != null) ? from : Instant.EPOCH;
        return ResponseEntity.ok(transactionService.getSummaryByType(jwt.getSubject(), start, end));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<Map<String, Object>>> getCategories(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        
        Instant end = (to != null) ? to : Instant.now();
        Instant start = (from != null) ? from : Instant.EPOCH;
        return ResponseEntity.ok(transactionService.getDebitSummaryByCategory(jwt.getSubject(), start, end));
    }
}
