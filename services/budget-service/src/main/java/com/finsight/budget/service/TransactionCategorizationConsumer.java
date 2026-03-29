package com.finsight.budget.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finsight.budget.domain.Budget;
import com.finsight.budget.event.TransactionCategorizedEvent;
import com.finsight.budget.persistence.BudgetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class TransactionCategorizationConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransactionCategorizationConsumer.class);

    private final ObjectMapper objectMapper;
    private final BudgetRepository budgetRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public TransactionCategorizationConsumer(ObjectMapper objectMapper, 
                                            BudgetRepository budgetRepository, 
                                            KafkaTemplate<String, String> kafkaTemplate) {
        this.objectMapper = objectMapper;
        this.budgetRepository = budgetRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "transactions.categorized", groupId = "budget-service-group")
    @Transactional
    public void onTransactionCategorized(String payloadStr) {
        try {
            TransactionCategorizedEvent event = objectMapper.readValue(payloadStr, TransactionCategorizedEvent.class);
            log.info("Received categorized transaction {} for category {} amt {}", event.transactionId(), event.categoryName(), event.amount());

            if (event.amount() == null || event.ownerEmail() == null || event.monthYear() == null || 
                event.amount().compareTo(BigDecimal.ZERO) >= 0) {
                // Ignore positive (credits/income) or malformed payload. Budgets track negative (spend).
                // Assuming amount is positive for income, negative for expense as designed in transaction service.
                return;
            }

            // The amount is negative for expense. We want current_spend as a positive magnitude.
            BigDecimal spendMagnitude = event.amount().abs();

            // 1. Try to find an existing budget for this category 
            // In the real world, categoryId is robust, here we match on Name if ID isn't set, 
            // but the request is categoryName. We'll find by name since ML only produces Name!
            // Wait, we need a method to find by CategoryName or ID. Let's just lookup by name.
            
            // Note: Since we don't have categoryId from ML, we'll iterate or write a custom query.
            // For now, let's just find all user budgets for the month and filter.
            Optional<Budget> matchedBudget = budgetRepository.findByOwnerEmailAndMonthYear(event.ownerEmail(), event.monthYear())
                .stream()
                .filter(b -> b.getCategoryName().equalsIgnoreCase(event.categoryName()))
                .findFirst();

            if (matchedBudget.isEmpty()) {
                log.debug("No budget found for category {}", event.categoryName());
                return;
            }

            Budget budget = matchedBudget.get();
            BigDecimal previousSpend = budget.getCurrentSpend();
            budget.addSpend(spendMagnitude);
            
            BigDecimal limit = budget.getLimitAmount();
            BigDecimal newSpend = budget.getCurrentSpend();
            
            budgetRepository.save(budget);
            
            // Check 90% threshold for alerting
            BigDecimal threshold = limit.multiply(new BigDecimal("0.90"));
            if (previousSpend.compareTo(threshold) < 0 && newSpend.compareTo(threshold) >= 0) {
                log.info("ALERT: Budget threshold breached for {} (Limit: {}, Spend: {})", event.categoryName(), limit, newSpend);
                
                String alertPayload = String.format("{\"ownerEmail\":\"%s\", \"categoryName\":\"%s\", \"limitAmount\":%s, \"currentSpend\":%s, \"type\":\"BUDGET_WARNING\"}",
                        event.ownerEmail(), event.categoryName(), limit, newSpend);
                        
                kafkaTemplate.send("budget.alerts", event.ownerEmail(), alertPayload);
            } else if (previousSpend.compareTo(limit) < 0 && newSpend.compareTo(limit) >= 0) {
                log.info("ALERT: Budget EXCEEDED for {} (Limit: {}, Spend: {})", event.categoryName(), limit, newSpend);
                
                String alertPayload = String.format("{\"ownerEmail\":\"%s\", \"categoryName\":\"%s\", \"limitAmount\":%s, \"currentSpend\":%s, \"type\":\"BUDGET_EXCEEDED\"}",
                        event.ownerEmail(), event.categoryName(), limit, newSpend);
                        
                kafkaTemplate.send("budget.alerts", event.ownerEmail(), alertPayload);
            }

        } catch (Exception e) {
            log.error("Failed to process categorization payload: {}", e.getMessage(), e);
            throw new RuntimeException("Kafka handling failed", e);
        }
    }
}
