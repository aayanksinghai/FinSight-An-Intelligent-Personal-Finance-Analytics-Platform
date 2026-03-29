package com.finsight.budget.api;

import com.finsight.budget.domain.Budget;
import java.math.BigDecimal;
import java.util.UUID;

public class BudgetResponse {
    private UUID id;
    private String categoryId;
    private String categoryName;
    private BigDecimal limitAmount;
    private BigDecimal currentSpend;
    private String monthYear;

    public BudgetResponse(Budget budget) {
        this.id = budget.getId();
        this.categoryId = budget.getCategoryId();
        this.categoryName = budget.getCategoryName();
        this.limitAmount = budget.getLimitAmount();
        this.currentSpend = budget.getCurrentSpend();
        this.monthYear = budget.getMonthYear();
    }

    public UUID getId() { return id; }
    public String getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public BigDecimal getLimitAmount() { return limitAmount; }
    public BigDecimal getCurrentSpend() { return currentSpend; }
    public String getMonthYear() { return monthYear; }
}
