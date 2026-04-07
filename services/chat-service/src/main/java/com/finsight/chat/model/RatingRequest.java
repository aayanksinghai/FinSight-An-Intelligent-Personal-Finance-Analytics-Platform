package com.finsight.chat.model;

public record RatingRequest(
    String messageId,
    String rating   // "HELPFUL" or "NOT_HELPFUL"
) {}
