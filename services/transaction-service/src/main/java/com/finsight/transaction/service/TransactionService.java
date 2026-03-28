package com.finsight.transaction.service;

import com.finsight.transaction.domain.Transaction;
import com.finsight.transaction.persistence.TransactionRepository;
import com.finsight.transaction.api.TransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public Page<TransactionResponse> list(String ownerEmail, Instant from, Instant to, String category, String type, int page, int size) {
        // Since we are building an MVP, we fetch everything ordered by date and paginate.
        // More sophisticated Specification filtering goes here in later phases.
        int safeSize = Math.max(1, Math.min(size, 100));
        Page<Transaction> txns = transactionRepository.findByOwnerEmailOrderByOccurredAtDesc(
            ownerEmail, PageRequest.of(Math.max(page, 0), safeSize)
        );
        return txns.map(TransactionResponse::from);
    }

    public TransactionResponse getById(String ownerEmail, UUID id) {
        return transactionRepository.findByIdAndOwnerEmail(id, ownerEmail)
                .map(TransactionResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
    }

    /**
     * Get aggregate sums by type (DEBIT vs CREDIT) for a date range.
     */
    public List<Map<String, Object>> getSummaryByType(String ownerEmail, Instant from, Instant to) {
        return transactionRepository.getSummaryByType(ownerEmail, from, to);
    }

    /**
     * Get aggregate sums of DEBITs broken down by Category for a date range.
     */
    public List<Map<String, Object>> getDebitSummaryByCategory(String ownerEmail, Instant from, Instant to) {
        return transactionRepository.getDebitSummaryByCategory(ownerEmail, from, to);
    }
}
