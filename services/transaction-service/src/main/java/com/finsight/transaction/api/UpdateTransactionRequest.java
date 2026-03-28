package com.finsight.transaction.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public record UpdateTransactionRequest(
        Instant occurredAt,
        @Pattern(regexp = "^(INCOME|EXPENSE)$", message = "type must be INCOME or EXPENSE") String type,
        @Size(max = 60) String category,
        @DecimalMin(value = "0.01", inclusive = true) BigDecimal amount,
        @Size(min = 3, max = 3) String currency,
        @Size(max = 255) String description,
        @Size(max = 120) String merchant) {
}

