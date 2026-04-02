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
    private final TransactionCategorizationService categorizationService;
    private final ObjectMapper objectMapper;

    public TransactionCategorizedConsumer(
            TransactionRepository transactionRepository,
            TransactionCategorizationService categorizationService,
            ObjectMapper objectMapper) {
        this.transactionRepository = transactionRepository;
        this.categorizationService = categorizationService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
        topics = "transactions.categorized",
        groupId = "${spring.kafka.consumer.group-id:transaction-service-group}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onTransactionCategorized(String payload) {
        try {
            TransactionCategorizedEvent event = objectMapper.readValue(payload, TransactionCategorizedEvent.class);
            log.debug("Received categorized event for transaction: {}", event.transactionId());

            if (event.transactionId() == null) {
                log.warn("Received categorized transaction event with null ID. Skipping. Payload: {}", payload);
                return;
            }

            // Retry mechanism to handle race condition with IngestionConsumer commit
            Transaction txn = null;
            for (int i = 0; i < 5; i++) {
                // By not being @Transactional at the method level, findById will hit the DB fresh
                txn = transactionRepository.findById(event.transactionId()).orElse(null);
                if (txn != null) break;
                
                log.info("Transaction {} not found yet in DB, retrying in 200ms... (attempt {})", event.transactionId(), i + 1);
                try {
                    Thread.sleep(200); 
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            if (txn == null) {
                log.warn("Transaction ID {} not found after multiple retries. Could not apply categorization.", event.transactionId());
                return;
            }

            categorizationService.updateTransactionCategory(txn, event);
            log.info("Successfully categorized transaction {} to {}", txn.getId(), event.categoryName());

        } catch (Exception e) {
            log.error("Failed to process categorized transaction payload: {}", e.getMessage(), e);
            throw new RuntimeException("Categorization event handling failed", e);
        }
    }
}
