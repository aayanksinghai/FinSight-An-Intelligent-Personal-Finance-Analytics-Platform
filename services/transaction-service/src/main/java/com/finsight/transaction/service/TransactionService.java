package com.finsight.transaction.service;

import com.finsight.transaction.api.CreateTransactionRequest;
import com.finsight.transaction.api.TransactionResponse;
import com.finsight.transaction.api.UpdateTransactionRequest;
import com.finsight.transaction.persistence.TransactionRecord;
import com.finsight.transaction.persistence.TransactionRepository;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public TransactionResponse create(String ownerEmail, CreateTransactionRequest request) {
        TransactionRecord record = new TransactionRecord();
        record.setOwnerEmail(normalizeEmail(ownerEmail));
        record.setOccurredAt(request.occurredAt());
        record.setType(normalizeType(request.type()));
        record.setCategory(normalizeText(request.category()));
        record.setAmount(request.amount());
        record.setCurrency(normalizeCurrency(request.currency()));
        record.setDescription(normalizeNullable(request.description()));
        record.setMerchant(normalizeNullable(request.merchant()));

        return toResponse(transactionRepository.save(record));
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> list(
            String ownerEmail,
            Instant from,
            Instant to,
            String category,
            String type,
            int page,
            int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "occurredAt"));

        String normalizedOwner = normalizeEmail(ownerEmail);
        Specification<TransactionRecord> spec = (root, query, cb) -> cb.equal(root.get("ownerEmail"), normalizedOwner);

        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("occurredAt"), to));
        }
        if (category != null && !category.isBlank()) {
            String categoryLike = "%" + category.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("category")), categoryLike));
        }
        if (type != null && !type.isBlank()) {
            String normalizedType = normalizeType(type);
            spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), normalizedType));
        }

        return transactionRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getById(String ownerEmail, UUID id) {
        return toResponse(requireOwnedTransaction(ownerEmail, id));
    }

    @Transactional
    public TransactionResponse update(String ownerEmail, UUID id, UpdateTransactionRequest request) {
        TransactionRecord record = requireOwnedTransaction(ownerEmail, id);

        if (request.occurredAt() != null) {
            record.setOccurredAt(request.occurredAt());
        }
        if (request.type() != null) {
            record.setType(normalizeType(request.type()));
        }
        if (request.category() != null) {
            record.setCategory(normalizeText(request.category()));
        }
        if (request.amount() != null) {
            record.setAmount(request.amount());
        }
        if (request.currency() != null) {
            record.setCurrency(normalizeCurrency(request.currency()));
        }
        if (request.description() != null) {
            record.setDescription(normalizeNullable(request.description()));
        }
        if (request.merchant() != null) {
            record.setMerchant(normalizeNullable(request.merchant()));
        }

        return toResponse(transactionRepository.save(record));
    }

    @Transactional
    public void delete(String ownerEmail, UUID id) {
        TransactionRecord record = requireOwnedTransaction(ownerEmail, id);
        transactionRepository.delete(record);
    }

    private TransactionRecord requireOwnedTransaction(String ownerEmail, UUID id) {
        return transactionRepository.findByIdAndOwnerEmail(id, normalizeEmail(ownerEmail))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
    }

    private TransactionResponse toResponse(TransactionRecord record) {
        return new TransactionResponse(
                record.getId(),
                record.getOwnerEmail(),
                record.getOccurredAt(),
                record.getType(),
                record.getCategory(),
                record.getAmount(),
                record.getCurrency(),
                record.getDescription(),
                record.getMerchant(),
                record.getCreatedAt(),
                record.getUpdatedAt());
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeType(String type) {
        return type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return "INR";
        }
        return currency.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

