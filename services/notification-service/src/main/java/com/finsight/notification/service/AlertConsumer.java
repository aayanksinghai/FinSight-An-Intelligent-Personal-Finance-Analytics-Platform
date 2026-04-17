package com.finsight.notification.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finsight.notification.domain.Notification;
import com.finsight.notification.domain.NotificationPreferenceId;
import com.finsight.notification.persistence.NotificationPreferenceRepository;
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
    private final NotificationPreferenceRepository preferenceRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public AlertConsumer(ObjectMapper objectMapper,
                         NotificationRepository notificationRepository,
                         NotificationPreferenceRepository preferenceRepository,
                         SimpMessagingTemplate messagingTemplate) {
        this.objectMapper = objectMapper;
        this.notificationRepository = notificationRepository;
        this.preferenceRepository = preferenceRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @KafkaListener(topics = "budget.alerts", groupId = "notification-service-group")
    @Transactional
    public void onBudgetAlert(String payloadStr) {
        try {
            JsonNode payload = objectMapper.readTree(payloadStr);
            String ownerEmail = payload.get("ownerEmail").asText();
            String category = payload.get("categoryName").asText();
            String type = payload.get("type").asText(); // BUDGET_WARNING or BUDGET_EXCEEDED

            double limit = payload.get("limitAmount").asDouble();
            double spend = payload.get("currentSpend").asDouble();

            String title;
            String message;
            if ("BUDGET_EXCEEDED".equals(type)) {
                title = "Budget Exceeded: " + category;
                message = String.format("You have exceeded your %s budget! Spent ₹%,.0f of ₹%,.0f limit.", category, spend, limit);
            } else {
                title = "Budget Warning: " + category;
                message = String.format("You are approaching your %s budget. Spent ₹%,.0f of ₹%,.0f limit.", category, spend, limit);
            }

            processAlert(ownerEmail, type, title, message);

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

            String merchant = payload.has("merchant") && !payload.get("merchant").isNull()
                    ? payload.get("merchant").asText() : "Unknown Merchant";
            double amount = payload.get("amount").asDouble();
            double score = payload.get("anomalyScore").asDouble();

            String title = "Unusual Transaction Detected";
            String message = String.format("Unusual transaction detected: ₹%,.0f at %s. Anomaly score: %.2f", amount, merchant, score);

            processAlert(ownerEmail, type, title, message);

        } catch (Exception e) {
            log.error("Failed to process anomaly alert: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "stress.score.alerts", groupId = "notification-service-group")
    @Transactional
    public void onStressScoreAlert(String payloadStr) {
        try {
            JsonNode payload = objectMapper.readTree(payloadStr);
            String ownerEmail = payload.get("ownerEmail").asText();
            String type = "STRESS_SCORE_CHANGE";
            double newScore = payload.get("newScore").asDouble();
            double oldScore = payload.get("previousScore").asDouble();
            double delta = newScore - oldScore;

            String title = "Financial Stress Score Changed";
            String direction = delta > 0 ? "increased" : "decreased";
            String message = String.format(
                    "Your financial stress score has %s by %.0f points to %.0f. %s",
                    direction, Math.abs(delta), newScore,
                    newScore > 70 ? "Consider reviewing your spending habits." : "Keep it up!"
            );

            processAlert(ownerEmail, type, title, message);

        } catch (Exception e) {
            log.error("Failed to process stress score alert: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "forecast.alerts", groupId = "notification-service-group")
    @Transactional
    public void onForecastAlert(String payloadStr) {
        try {
            JsonNode payload = objectMapper.readTree(payloadStr);
            String ownerEmail = payload.get("ownerEmail").asText();
            String type = "FORECAST_UPDATE";
            String monthYear = payload.has("monthYear") ? payload.get("monthYear").asText() : "next month";

            String title = "Forecast Update Available";
            String message = String.format("Your spending forecast for %s has been updated. Check your dashboard for the latest projections.", monthYear);

            processAlert(ownerEmail, type, title, message);

        } catch (Exception e) {
            log.error("Failed to process forecast alert: {}", e.getMessage(), e);
        }
    }

    // ── Private helper ──────────────────────────────────────────────────────

    private void processAlert(String ownerEmail, String type, String title, String message) {
        log.info("Processing alert for {}: [{}] {}", ownerEmail, type, message);

        // Check user preferences — skip if disabled
        boolean enabled = preferenceRepository
                .findById(new NotificationPreferenceId(ownerEmail, type))
                .map(pref -> pref.isEnabled())
                .orElse(true); // default enabled if no preference row exists

        if (!enabled) {
            log.debug("Notification type {} is disabled for user {}. Skipping.", type, ownerEmail);
            return;
        }

        // Save to DB
        Notification notif = new Notification(ownerEmail, type, title, message);
        notificationRepository.save(notif);

        // Dispatch via WebSocket to specific user
        messagingTemplate.convertAndSendToUser(ownerEmail, "/queue/notifications", notif);
        log.debug("Dispatched alert to WebSocket for user: {}", ownerEmail);
    }
}
