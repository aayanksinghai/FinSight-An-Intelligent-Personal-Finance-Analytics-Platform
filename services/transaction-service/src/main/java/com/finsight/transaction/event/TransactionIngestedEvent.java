package com.finsight.transaction.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event representation received from ingestion-service.
 */
public record TransactionIngestedEvent(
    String eventId,
    String ownerEmail,
    String jobId,
    String sourceFileName,
    Instant occurredAt,
    String rawDescription,
    String merchant,
    BigDecimal debitAmount,
    BigDecimal creditAmount,
    BigDecimal balance,
    String currency,
    String sourceBank,
    String rawText
) {
}
