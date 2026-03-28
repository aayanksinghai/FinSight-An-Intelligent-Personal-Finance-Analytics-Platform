package com.finsight.transaction.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Event published by transaction-service when a new raw transaction is saved.
 */
public record TransactionCreatedEvent(
    UUID id,
    String ownerEmail,
    String rawDescription,
    String merchant,
    BigDecimal amount,
    String type,
    Instant occurredAt
) {
}
