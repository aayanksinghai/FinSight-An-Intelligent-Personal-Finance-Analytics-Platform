package com.finsight.transaction.event;

import java.util.UUID;

/**
 * Event published by categorization-service containing the inferred category name.
 */
public record TransactionCategorizedEvent(
    UUID transactionId,
    String categoryName,
    String merchant,
    Double confidenceScore
) {
}
