package com.finsight.chat.model;

import java.time.Instant;

public record ChatMessageDto(
    String id,
    String role,      // "USER" or "ASSISTANT"
    String content,
    String intent,
    String rating,    // null, "HELPFUL", or "NOT_HELPFUL"
    Instant createdAt
) {}
