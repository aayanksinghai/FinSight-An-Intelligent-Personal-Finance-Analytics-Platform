package com.finsight.transaction.api;

import com.finsight.transaction.persistence.TransactionRepository;
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

    private final TransactionRepository transactionRepository;

    public AdminMetricsController(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @GetMapping("/transactions")
    public ResponseEntity<Map<String, Long>> getTransactionMetrics(@AuthenticationPrincipal Jwt jwt) {
        requireAdmin(jwt);
        long totalTransactions = transactionRepository.count();
        return ResponseEntity.ok(Map.of("totalTransactions", totalTransactions));
    }

    private void requireAdmin(Jwt jwt) {
        if (jwt == null || !"ADMIN".equalsIgnoreCase(jwt.getClaimAsString("role"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role required");
        }
    }
}
