package com.finsight.transaction.service;

import com.finsight.transaction.domain.Category;
import com.finsight.transaction.domain.Transaction;
import com.finsight.transaction.event.TransactionIngestedEvent;
import com.finsight.transaction.event.TransactionCreatedEvent;
import com.finsight.transaction.persistence.CategoryRepository;
import com.finsight.transaction.persistence.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class TransactionIngestionConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransactionIngestionConsumer.class);

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final ObjectMapper objectMapper;
    private final org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;

    public TransactionIngestionConsumer(
            TransactionRepository transactionRepository,
            CategoryRepository categoryRepository,
            ObjectMapper objectMapper,
            org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(
        topics = "${ingestion.kafka.topic:transactions.ingested}",
        groupId = "${spring.kafka.consumer.group-id:transaction-service-group}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onTransactionIngested(String payload) {
        try {
            TransactionIngestedEvent event = objectMapper.readValue(payload, TransactionIngestedEvent.class);
            log.debug("Received event: {} / {}", event.eventId(), event.ownerEmail());

            // 1) Find the Uncategorized category as default
            Category defaultCategory = categoryRepository.findByNameIgnoreCase("Uncategorized")
                .orElse(null);

            // 2) Convert event to Transaction entity
            Transaction txn = new Transaction();
            // We use the event ID as a deduplication token if possible, or generate a new UUID.
            // For now, generate new UUID as our primary key.
            txn.setId(UUID.randomUUID());
            txn.setOwnerEmail(event.ownerEmail());
            txn.setJobId(event.jobId());
            txn.setSourceFileName(event.sourceFileName());
            txn.setSourceBank(event.sourceBank());
            txn.setOccurredAt(event.occurredAt());
            txn.setRawDescription(event.rawDescription());
            txn.setNormalizedMerchant(event.merchant());
            txn.setCategory(defaultCategory);

            // 3) Determine Type and Amount
            if (event.debitAmount() != null) {
                txn.setAmount(event.debitAmount());
                txn.setType("DEBIT");
            } else if (event.creditAmount() != null) {
                txn.setAmount(event.creditAmount());
                txn.setType("CREDIT");
            } else {
                log.warn("Event {} has no debit or credit amount. Skipping.", event.eventId());
                return;
            }

            txn.setCurrency(event.currency() != null ? event.currency() : "INR");
            txn.setBalanceAfter(event.balance());
            txn.setRawText(event.rawText());
            txn.setCreatedAt(Instant.now());
            txn.setUpdatedAt(Instant.now());

            transactionRepository.save(txn);
            
            log.debug("Saved transaction for user {} amount {} {}", 
                txn.getOwnerEmail(), txn.getAmount(), txn.getType());

            // Publish to transactions.created for ML ML Categorization service
            TransactionCreatedEvent createdEvent = new TransactionCreatedEvent(
                txn.getId(), txn.getOwnerEmail(), txn.getRawDescription(), 
                txn.getNormalizedMerchant(), txn.getAmount(), txn.getType(), txn.getOccurredAt());
            
            String eventJson = objectMapper.writeValueAsString(createdEvent);
            kafkaTemplate.send("transactions.created", txn.getId().toString(), eventJson);

        } catch (Exception e) {
            log.error("Failed to process transaction payload string: {}", e.getMessage(), e);
            throw new RuntimeException("Kafka handling failed", e); // trigger retry
        }
    }
}
