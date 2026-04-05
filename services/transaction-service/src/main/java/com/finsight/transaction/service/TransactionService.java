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
        return list(ownerEmail, from, to, category, type, null, page, size);
    }

    public Page<TransactionResponse> list(String ownerEmail, Instant from, Instant to, String category, String type, String search, int page, int size) {
        int safeSize = Math.max(1, Math.min(size, 100));
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "occurredAt"));

        org.springframework.data.jpa.domain.Specification<com.finsight.transaction.domain.Transaction> spec =
            org.springframework.data.jpa.domain.Specification
                .where(com.finsight.transaction.persistence.TransactionSpecifications.ownedBy(ownerEmail))
                .and(com.finsight.transaction.persistence.TransactionSpecifications.ofType(type))
                .and(com.finsight.transaction.persistence.TransactionSpecifications.afterDate(from))
                .and(com.finsight.transaction.persistence.TransactionSpecifications.beforeDate(to))
                .and(com.finsight.transaction.persistence.TransactionSpecifications.inCategory(category))
                .and(com.finsight.transaction.persistence.TransactionSpecifications.keywordSearch(search));

        return transactionRepository.findAll(spec, pageRequest).map(TransactionResponse::from);
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
