package com.finsight.chat.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "role", nullable = false)
    private String role; // "USER" or "ASSISTANT"

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "intent")
    private String intent;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public ChatMessage() {}

    public ChatMessage(UUID sessionId, String role, String content, String intent) {
        this.sessionId = sessionId;
        this.role = role;
        this.content = content;
        this.intent = intent;
    }

    public UUID getId() { return id; }
    public UUID getSessionId() { return sessionId; }
    public String getRole() { return role; }
    public String getContent() { return content; }
    public String getIntent() { return intent; }
    public Instant getCreatedAt() { return createdAt; }
}
