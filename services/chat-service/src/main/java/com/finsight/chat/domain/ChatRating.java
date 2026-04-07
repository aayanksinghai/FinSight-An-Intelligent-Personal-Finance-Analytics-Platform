package com.finsight.chat.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_ratings")
public class ChatRating {

    @Id
    @Column(name = "message_id")
    private UUID messageId;

    @Column(name = "rating", nullable = false)
    private String rating; // "HELPFUL" or "NOT_HELPFUL"

    @Column(name = "rated_at", nullable = false)
    private Instant ratedAt = Instant.now();

    public ChatRating() {}

    public ChatRating(UUID messageId, String rating) {
        this.messageId = messageId;
        this.rating = rating;
    }

    public UUID getMessageId() { return messageId; }
    public String getRating() { return rating; }
    public Instant getRatedAt() { return ratedAt; }
    public void setRating(String rating) { this.rating = rating; }
}
