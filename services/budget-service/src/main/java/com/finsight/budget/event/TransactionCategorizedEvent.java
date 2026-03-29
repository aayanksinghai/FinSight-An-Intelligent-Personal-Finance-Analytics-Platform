package com.finsight.budget.event;

import java.math.BigDecimal;
import java.util.UUID;

public record TransactionCategorizedEvent(
    UUID transactionId,
    String categoryName,
    String merchant,
    Double confidenceScore,
    BigDecimal amount,
    String ownerEmail,
    String monthYear
) {
}
