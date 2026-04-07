package com.finsight.chat.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_sessions")
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_email", nullable = false)
    private String ownerEmail;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public ChatSession() {}

    public ChatSession(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public UUID getId() { return id; }
    public String getOwnerEmail() { return ownerEmail; }
    public Instant getCreatedAt() { return createdAt; }
}
