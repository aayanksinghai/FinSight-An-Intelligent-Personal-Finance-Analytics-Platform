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
            log.info("Received categorized transaction {} for category {} amt {}",
                    event.transactionId(), event.categoryName(), event.amount());

            // Skip events with missing essential data
            if (event.amount() == null || event.ownerEmail() == null || event.monthYear() == null) {
                log.warn("Skipping event with missing data: txnId={}", event.transactionId());
                return;
            }

            // Skip income/credit transactions (negative amounts) and "Uncategorized" - we only track spending
            if (event.amount().compareTo(BigDecimal.ZERO) <= 0) {
                log.debug("Skipping non-debit transaction amount={}", event.amount());
                return;
            }

            if ("Uncategorized".equalsIgnoreCase(event.categoryName())) {
                log.debug("Skipping uncategorized transaction {}", event.transactionId());
                return;
            }

            // Amount from ML service is positive for DEBIT - this is the spend magnitude
            BigDecimal spendMagnitude = event.amount().abs();

            // Find matching budget for this user/category/month
            Optional<Budget> matchedBudget = budgetRepository
                    .findByOwnerEmailAndMonthYear(event.ownerEmail(), event.monthYear())
                    .stream()
                    .filter(b -> b.getCategoryName().equalsIgnoreCase(event.categoryName()))
                    .findFirst();

            if (matchedBudget.isEmpty()) {
                log.debug("No budget found for category '{}' user={} month={}",
                        event.categoryName(), event.ownerEmail(), event.monthYear());
                return;
            }

            Budget budget = matchedBudget.get();
            BigDecimal previousSpend = budget.getCurrentSpend();
            budget.addSpend(spendMagnitude);

            BigDecimal limit = budget.getLimitAmount();
            BigDecimal newSpend = budget.getCurrentSpend();

            budgetRepository.save(budget);
            log.info("Updated budget '{}' for {}: spend {} -> {} (limit {})",
                    event.categoryName(), event.ownerEmail(), previousSpend, newSpend, limit);

            // Alert on 90% threshold breach (first time crossing)
            BigDecimal threshold = limit.multiply(new BigDecimal("0.90"));
            if (previousSpend.compareTo(threshold) < 0 && newSpend.compareTo(threshold) >= 0) {
                log.info("ALERT: Budget threshold 90% breached for {} (Limit: {}, Spend: {})",
                        event.categoryName(), limit, newSpend);
                String alertPayload = String.format(
                        "{\"ownerEmail\":\"%s\",\"categoryName\":\"%s\",\"limitAmount\":%s,\"currentSpend\":%s,\"type\":\"BUDGET_WARNING\"}",
                        event.ownerEmail(), event.categoryName(), limit, newSpend);
                kafkaTemplate.send("budget.alerts", event.ownerEmail(), alertPayload);
            } else if (previousSpend.compareTo(limit) < 0 && newSpend.compareTo(limit) >= 0) {
                log.info("ALERT: Budget EXCEEDED for {} (Limit: {}, Spend: {})",
                        event.categoryName(), limit, newSpend);
                String alertPayload = String.format(
                        "{\"ownerEmail\":\"%s\",\"categoryName\":\"%s\",\"limitAmount\":%s,\"currentSpend\":%s,\"type\":\"BUDGET_EXCEEDED\"}",
                        event.ownerEmail(), event.categoryName(), limit, newSpend);
                kafkaTemplate.send("budget.alerts", event.ownerEmail(), alertPayload);
            }

        } catch (Exception e) {
            log.error("Failed to process categorization payload: {}", e.getMessage(), e);
            throw new RuntimeException("Kafka handling failed", e);
        }
    }
}
