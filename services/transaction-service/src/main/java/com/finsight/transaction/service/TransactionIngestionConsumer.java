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

    private final TransactionPersistenceService persistenceService;
    private final ObjectMapper objectMapper;
    private final org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;

    public TransactionIngestionConsumer(
            TransactionPersistenceService persistenceService,
            ObjectMapper objectMapper,
            org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate) {
        this.persistenceService = persistenceService;
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(
        topics = "${ingestion.kafka.topic:transactions.ingested}",
        groupId = "${spring.kafka.consumer.group-id:transaction-service-group}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onTransactionIngested(String payload) {
        try {
            TransactionIngestedEvent event = objectMapper.readValue(payload, TransactionIngestedEvent.class);
            log.debug("Received event: {} / {}", event.eventId(), event.ownerEmail());

            // 1) Save to DB (Transaction happens inside this call and COMMITS when it returns)
            Transaction txn = persistenceService.saveIngestedTransaction(event);
            
            if (txn == null) {
                log.warn("Event {} skipped or failed to save.", event.eventId());
                return;
            }
            
            log.info("Saved and Committed transaction for user {} amount {} {} (ID: {})", 
                txn.getOwnerEmail(), txn.getAmount(), txn.getType(), txn.getId());

            // 2) Publish to Kafka only AFTER the DB commit is guaranteed
            TransactionCreatedEvent createdEvent = new TransactionCreatedEvent(
                txn.getId(), txn.getOwnerEmail(), txn.getRawDescription(), 
                txn.getNormalizedMerchant(), txn.getAmount(), txn.getType(), txn.getOccurredAt());
            
            String eventJson = objectMapper.writeValueAsString(createdEvent);
            kafkaTemplate.send("transactions.created", txn.getId().toString(), eventJson);

        } catch (Exception e) {
            log.error("Failed to process transaction payload string: {}", e.getMessage(), e);
            throw new RuntimeException("Kafka handling failed", e);
        }
    }
}
