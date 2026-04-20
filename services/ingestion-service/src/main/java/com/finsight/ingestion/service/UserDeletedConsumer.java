package com.finsight.ingestion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finsight.ingestion.persistence.IngestionJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class UserDeletedConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserDeletedConsumer.class);

    private final IngestionJobRepository ingestionJobRepository;
    private final ObjectMapper objectMapper;

    public UserDeletedConsumer(IngestionJobRepository ingestionJobRepository, ObjectMapper objectMapper) {
        this.ingestionJobRepository = ingestionJobRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
        topics = "user.events",
        groupId = "${spring.kafka.consumer.group-id:ingestion-service-group}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onUserDeleted(String payload) {
        try {
            JsonNode event = objectMapper.readTree(payload);
            if (event.has("eventType") && "user.deleted".equals(event.get("eventType").asText())) {
                String email = event.get("email").asText();
                log.info("Received user.deleted event for email: {}", email);
                ingestionJobRepository.deleteByOwnerEmail(email);
            }
        } catch (Exception e) {
            log.error("Failed to process user.events payload: {}", payload, e);
        }
    }
}
