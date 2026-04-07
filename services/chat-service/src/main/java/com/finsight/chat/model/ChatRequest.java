package com.finsight.chat.model;

public record ChatRequest(
    String sessionId,
    String content
) {}
