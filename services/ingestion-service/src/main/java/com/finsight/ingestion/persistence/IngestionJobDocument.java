package com.finsight.ingestion.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

/**
 * MongoDB document tracking the lifecycle of a single bank statement upload job.
 */
@Document(collection = "ingestion_jobs")
public class IngestionJobDocument {

    public enum Status {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }

    @Id
    private String id;

    @Indexed
    @Field("owner_email")
    private String ownerEmail;

    @Field("file_name")
    private String fileName;

    @Field("file_size_bytes")
    private long fileSizeBytes;

    @Field("content_type")
    private String contentType;

    @Field("status")
    private Status status;

    /** Detected bank name from header / format analysis. */
    @Field("detected_bank")
    private String detectedBank;

    /** Total rows successfully parsed and published to Kafka. */
    @Field("rows_parsed")
    private int rowsParsed;

    /** Total rows attempted (including skipped/header rows). */
    @Field("rows_total")
    private int rowsTotal;

    /** Human-readable error message if status = FAILED. */
    @Field("error_message")
    private String errorMessage;

    @Field("created_at")
    private Instant createdAt;

    @Field("started_at")
    private Instant startedAt;

    @Field("completed_at")
    private Instant completedAt;

    // ── Factory ──────────────────────────────────────────────────────────────

    public static IngestionJobDocument create(
            String ownerEmail,
            String fileName,
            long fileSizeBytes,
            String contentType) {
        IngestionJobDocument doc = new IngestionJobDocument();
        doc.ownerEmail = ownerEmail;
        doc.fileName = fileName;
        doc.fileSizeBytes = fileSizeBytes;
        doc.contentType = contentType;
        doc.status = Status.PENDING;
        doc.rowsParsed = 0;
        doc.rowsTotal = 0;
        doc.createdAt = Instant.now();
        return doc;
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getDetectedBank() { return detectedBank; }
    public void setDetectedBank(String detectedBank) { this.detectedBank = detectedBank; }

    public int getRowsParsed() { return rowsParsed; }
    public void setRowsParsed(int rowsParsed) { this.rowsParsed = rowsParsed; }

    public int getRowsTotal() { return rowsTotal; }
    public void setRowsTotal(int rowsTotal) { this.rowsTotal = rowsTotal; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
