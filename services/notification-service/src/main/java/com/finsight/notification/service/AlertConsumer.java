package com.finsight.notification.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finsight.notification.domain.Notification;
import com.finsight.notification.persistence.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertConsumer {

    private static final Logger log = LoggerFactory.getLogger(AlertConsumer.class);

    private final ObjectMapper objectMapper;
    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public AlertConsumer(ObjectMapper objectMapper,
                         NotificationRepository notificationRepository,
                         SimpMessagingTemplate messagingTemplate) {
        this.objectMapper = objectMapper;
        this.notificationRepository = notificationRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @KafkaListener(topics = "budget.alerts", groupId = "notification-service-group")
    @Transactional
    public void onBudgetAlert(String payloadStr) {
        try {
            JsonNode payload = objectMapper.readTree(payloadStr);
            String ownerEmail = payload.get("ownerEmail").asText();
            String category = payload.get("categoryName").asText();
            String type = payload.get("type").asText();
            
            // Format INR properly using String.format (simpler approach for Java backend)
            double limit = payload.get("limitAmount").asDouble();
            double spend = payload.get("currentSpend").asDouble();
            
            String message;
            if ("BUDGET_EXCEEDED".equals(type)) {
                message = String.format("You have exceeded your %s budget! Spent ₹%,.0f of ₹%,.0f limit.", category, spend, limit);
            } else {
                message = String.format("You are approaching your %s budget. Spent ₹%,.0f of ₹%,.0f limit.", category, spend, limit);
            }

            processAlert(ownerEmail, type, message);

        } catch (Exception e) {
            log.error("Failed to process budget alert: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "anomaly.alerts", groupId = "notification-service-group")
    @Transactional
    public void onAnomalyAlert(String payloadStr) {
        try {
            JsonNode payload = objectMapper.readTree(payloadStr);
            String ownerEmail = payload.get("ownerEmail").asText();
            String type = "ANOMALY_DETECTED";
            
            // Expected payload from Python Anomaly Service
            String merchant = payload.has("merchant") && !payload.get("merchant").isNull() ? payload.get("merchant").asText() : "Unknown Merchant";
            double amount = payload.get("amount").asDouble();
            double score = payload.get("anomalyScore").asDouble();
            
            String message = String.format("Unusual transaction detected: ₹%,.0f at %s. Anomaly score: %.2f", amount, merchant, score);

            processAlert(ownerEmail, type, message);

        } catch (Exception e) {
            log.error("Failed to process anomaly alert: {}", e.getMessage(), e);
        }
    }

    private void processAlert(String ownerEmail, String type, String message) {
        log.info("Processing alert for {}: [{}] {}", ownerEmail, type, message);
        
        // 1. Save to DB
        Notification notif = new Notification(ownerEmail, type, message);
        notificationRepository.save(notif);
        
        // 2. Dispatch via WebSocket to specific user
        // STOMP endpoint expected on frontend: /user/queue/notifications
        messagingTemplate.convertAndSendToUser(ownerEmail, "/queue/notifications", notif);
        log.debug("Dispatched alert to WebSocket topic for user: {}", ownerEmail);
    }
}
