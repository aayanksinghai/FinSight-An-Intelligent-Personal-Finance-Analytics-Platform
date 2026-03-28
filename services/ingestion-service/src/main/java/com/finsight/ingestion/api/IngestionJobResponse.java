package com.finsight.ingestion.api;

import com.finsight.ingestion.persistence.IngestionJobDocument;

import java.time.Instant;

/**
 * API response DTO for an ingestion job. Never exposes internal MongoDB structure directly.
 */
public record IngestionJobResponse(
    String jobId,
    String ownerEmail,
    String fileName,
    long fileSizeBytes,
    String contentType,
    String status,
    String detectedBank,
    int rowsParsed,
    int rowsTotal,
    String errorMessage,
    Instant createdAt,
    Instant startedAt,
    Instant completedAt
) {
    public static IngestionJobResponse from(IngestionJobDocument doc) {
        return new IngestionJobResponse(
            doc.getId(),
            doc.getOwnerEmail(),
            doc.getFileName(),
            doc.getFileSizeBytes(),
            doc.getContentType(),
            doc.getStatus().name(),
            doc.getDetectedBank(),
            doc.getRowsParsed(),
            doc.getRowsTotal(),
            doc.getErrorMessage(),
            doc.getCreatedAt(),
            doc.getStartedAt(),
            doc.getCompletedAt()
        );
    }
}
