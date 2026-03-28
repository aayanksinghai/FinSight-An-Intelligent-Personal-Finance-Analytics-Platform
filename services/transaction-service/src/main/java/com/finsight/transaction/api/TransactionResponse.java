package com.finsight.transaction.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        String ownerEmail,
        Instant occurredAt,
        String type,
        String category,
        BigDecimal amount,
        String currency,
        String description,
        String merchant,
        Instant createdAt,
        Instant updatedAt) {
}

