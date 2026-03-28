package com.finsight.transaction.api;

import com.finsight.transaction.domain.Category;
import com.finsight.transaction.domain.Transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TransactionResponse(
    UUID id,
    String ownerEmail,
    Instant occurredAt,
    String description,
    String merchant,
    String categoryName,
    String categoryColor,
    String categoryIcon,
    BigDecimal amount,
    String type,
    String currency,
    List<String> tags,
    Boolean isAnomaly
) {
    public static TransactionResponse from(Transaction t) {
        Category c = t.getCategory();
        return new TransactionResponse(
            t.getId(),
            t.getOwnerEmail(),
            t.getOccurredAt(),
            t.getRawDescription(),
            t.getNormalizedMerchant(),
            c != null ? c.getName() : null,
            c != null ? c.getColor() : null,
            c != null ? c.getIcon() : null,
            t.getAmount(),
            t.getType(),
            t.getCurrency(),
            t.getTags() != null ? t.getTags() : List.of(),
            t.getIsAnomaly()
        );
    }
}
