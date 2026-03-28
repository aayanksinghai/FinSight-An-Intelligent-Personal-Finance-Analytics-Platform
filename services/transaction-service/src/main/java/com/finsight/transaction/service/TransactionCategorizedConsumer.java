package com.finsight.transaction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finsight.transaction.domain.Category;
import com.finsight.transaction.domain.Transaction;
import com.finsight.transaction.event.TransactionCategorizedEvent;
import com.finsight.transaction.persistence.CategoryRepository;
import com.finsight.transaction.persistence.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionCategorizedConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransactionCategorizedConsumer.class);

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final ObjectMapper objectMapper;

    public TransactionCategorizedConsumer(
            TransactionRepository transactionRepository,
            CategoryRepository categoryRepository,
            ObjectMapper objectMapper) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
        topics = "transactions.categorized",
        groupId = "${spring.kafka.consumer.group-id:transaction-service-group}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onTransactionCategorized(String payload) {
        try {
            TransactionCategorizedEvent event = objectMapper.readValue(payload, TransactionCategorizedEvent.class);
            log.debug("Received categorized event for transaction: {}", event.transactionId());

            Transaction txn = transactionRepository.findById(event.transactionId())
                .orElse(null);

            if (txn == null) {
                log.warn("Transaction ID {} not found. Could not apply categorization.", event.transactionId());
                return;
            }

            // Find category by name
            if (event.categoryName() != null && !event.categoryName().isBlank()) {
                Category category = categoryRepository.findByNameIgnoreCase(event.categoryName())
                    .orElse(null);
                
                if (category != null) {
                    txn.setCategory(category);
                } else {
                    log.warn("Category name '{}' from ML service not found in database. Keeping existing category.", event.categoryName());
                }
            }

            // Update merchant if provided by ML model
            if (event.merchant() != null && !event.merchant().isBlank()) {
                txn.setNormalizedMerchant(event.merchant());
            }

            transactionRepository.save(txn);
            log.debug("Successfully categorized transaction {} to {}", txn.getId(), event.categoryName());

        } catch (Exception e) {
            log.error("Failed to process categorized transaction payload: {}", e.getMessage(), e);
            throw new RuntimeException("Categorization event handling failed", e);
        }
    }
}
