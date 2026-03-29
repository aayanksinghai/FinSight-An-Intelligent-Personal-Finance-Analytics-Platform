package com.finsight.budget.service;

import com.finsight.budget.api.BudgetRequest;
import com.finsight.budget.api.BudgetResponse;
import com.finsight.budget.domain.Budget;
import com.finsight.budget.persistence.BudgetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;

    public BudgetService(BudgetRepository budgetRepository) {
        this.budgetRepository = budgetRepository;
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> getBudgetsForMonth(String ownerEmail, String monthYear) {
        return budgetRepository.findByOwnerEmailAndMonthYear(ownerEmail, monthYear)
                .stream()
                .map(BudgetResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public BudgetResponse upsertBudget(String ownerEmail, BudgetRequest request) {
        Optional<Budget> existing = budgetRepository.findByOwnerEmailAndCategoryIdAndMonthYear(
                ownerEmail, request.getCategoryId(), request.getMonthYear());

        Budget budget;
        if (existing.isPresent()) {
            budget = existing.get();
            budget.setLimitAmount(request.getLimitAmount());
            budget.setCategoryName(request.getCategoryName());
        } else {
            budget = new Budget(
                    UUID.randomUUID(),
                    ownerEmail,
                    request.getCategoryId(),
                    request.getCategoryName(),
                    request.getLimitAmount(),
                    request.getMonthYear()
            );
        }
        
        return new BudgetResponse(budgetRepository.save(budget));
    }
}
