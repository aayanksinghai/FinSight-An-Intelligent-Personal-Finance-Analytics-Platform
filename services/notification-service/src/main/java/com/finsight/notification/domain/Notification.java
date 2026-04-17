package com.finsight.notification.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    private UUID id;

    private String ownerEmail;

    private String type; // BUDGET_WARNING, BUDGET_EXCEEDED, ANOMALY_DETECTED, STRESS_SCORE_CHANGE

    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_read")
    private boolean isRead;

    private Instant createdAt;

    protected Notification() {}

    public Notification(String ownerEmail, String type, String title, String message) {
        this.id = UUID.randomUUID();
        this.ownerEmail = ownerEmail;
        this.type = type;
        this.title = title;
        this.message = message;
        this.isRead = false;
        this.createdAt = Instant.now();
    }

    public void markAsRead() {
        this.isRead = true;
    }

    public UUID getId() { return id; }
    public String getOwnerEmail() { return ownerEmail; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public boolean isRead() { return isRead; }
    public Instant getCreatedAt() { return createdAt; }
}
