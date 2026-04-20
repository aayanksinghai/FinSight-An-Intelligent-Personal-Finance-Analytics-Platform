package com.finsight.transaction.api;

import com.finsight.transaction.domain.Transaction;
import com.finsight.transaction.persistence.TransactionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/internal/transactions")
public class InternalTransactionController {

    private final TransactionRepository transactionRepository;

    public InternalTransactionController(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @PostMapping("/check-duplicates")
    public ResponseEntity<List<String>> checkDuplicates(@RequestBody List<String> hashes) {
        if (hashes == null || hashes.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        
        List<com.finsight.transaction.domain.Transaction> existing = transactionRepository.findByContentHashIn(hashes);
        List<String> duplicateHashes = existing.stream()
                .map(com.finsight.transaction.domain.Transaction::getContentHash)
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(duplicateHashes);
    }
}
