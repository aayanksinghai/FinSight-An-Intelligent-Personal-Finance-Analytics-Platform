package com.finsight.chat.model;

public record ChatResponse(
    String sessionId,
    String messageId,
    String content,
    String intent
) {}
