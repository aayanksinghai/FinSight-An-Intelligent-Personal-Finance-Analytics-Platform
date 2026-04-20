package com.finsight.budget.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finsight.budget.persistence.BudgetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDeletedConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserDeletedConsumer.class);

    private final BudgetRepository budgetRepository;
    private final ObjectMapper objectMapper;

    public UserDeletedConsumer(BudgetRepository budgetRepository, ObjectMapper objectMapper) {
        this.budgetRepository = budgetRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
        topics = "user.events",
        groupId = "${spring.kafka.consumer.group-id:budget-service-group}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onUserDeleted(String payload) {
        try {
            JsonNode event = objectMapper.readTree(payload);
            if (event.has("eventType") && "user.deleted".equals(event.get("eventType").asText())) {
                String email = event.get("email").asText();
                log.info("Received user.deleted event for email: {}", email);
                budgetRepository.deleteByOwnerEmail(email);
            }
        } catch (Exception e) {
            log.error("Failed to process user.events payload: {}", payload, e);
        }
    }
}
