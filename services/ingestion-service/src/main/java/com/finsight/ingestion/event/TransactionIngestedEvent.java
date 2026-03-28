package com.finsight.ingestion.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Kafka event published to {@code transactions.ingested} for each parsed row.
 * Downstream consumers (transaction-service, categorization-service, anomaly-detection-service)
 * subscribe to this topic.
 *
 * <p>All fields are nullable — the parser emits best-effort values; downstream
 * services validate and normalise further.</p>
 */
public class TransactionIngestedEvent {

    /** Unique event ID — used for idempotent downstream processing. */
    private String eventId;

    /** Owner user email (from JWT subject). */
    private String ownerEmail;

    /** MongoDB job ID that produced this event. */
    private String jobId;

    /** Source file name for audit / deduplication. */
    private String sourceFileName;

    /** ISO-8601 transaction date parsed from the statement. */
    private Instant occurredAt;

    /** Raw description / narration text from the statement. */
    private String rawDescription;

    /** Normalized merchant name (best-effort from description). */
    private String merchant;

    /** Debit amount (positive) — null if credit row. */
    private BigDecimal debitAmount;

    /** Credit amount (positive) — null if debit row. */
    private BigDecimal creditAmount;

    /** Running balance after this transaction (if present in statement). */
    private BigDecimal balance;

    /** Currency code detected or defaulted to INR. */
    private String currency;

    /** Name of the originating bank (detected from file format/headers). */
    private String sourceBank;

    /** Raw text row as extracted from the file — for downstream re-parsing. */
    private String rawText;

    /** UTC timestamp when this event was published. */
    private Instant publishedAt;

    // ── No-arg constructor (required by Jackson) ─────────────────────────────

    public TransactionIngestedEvent() {
    }

    // ── Builder-style factory ────────────────────────────────────────────────

    public static TransactionIngestedEvent of(
            String ownerEmail,
            String jobId,
            String sourceFileName,
            Instant occurredAt,
            String rawDescription,
            String merchant,
            BigDecimal debitAmount,
            BigDecimal creditAmount,
            BigDecimal balance,
            String currency,
            String sourceBank,
            String rawText) {
        TransactionIngestedEvent e = new TransactionIngestedEvent();
        e.eventId = UUID.randomUUID().toString();
        e.ownerEmail = ownerEmail;
        e.jobId = jobId;
        e.sourceFileName = sourceFileName;
        e.occurredAt = occurredAt;
        e.rawDescription = rawDescription;
        e.merchant = merchant;
        e.debitAmount = debitAmount;
        e.creditAmount = creditAmount;
        e.balance = balance;
        e.currency = currency == null || currency.isBlank() ? "INR" : currency.trim().toUpperCase();
        e.sourceBank = sourceBank;
        e.rawText = rawText;
        e.publishedAt = Instant.now();
        return e;
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public String getSourceFileName() { return sourceFileName; }
    public void setSourceFileName(String sourceFileName) { this.sourceFileName = sourceFileName; }

    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }

    public String getRawDescription() { return rawDescription; }
    public void setRawDescription(String rawDescription) { this.rawDescription = rawDescription; }

    public String getMerchant() { return merchant; }
    public void setMerchant(String merchant) { this.merchant = merchant; }

    public BigDecimal getDebitAmount() { return debitAmount; }
    public void setDebitAmount(BigDecimal debitAmount) { this.debitAmount = debitAmount; }

    public BigDecimal getCreditAmount() { return creditAmount; }
    public void setCreditAmount(BigDecimal creditAmount) { this.creditAmount = creditAmount; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getSourceBank() { return sourceBank; }
    public void setSourceBank(String sourceBank) { this.sourceBank = sourceBank; }

    public String getRawText() { return rawText; }
    public void setRawText(String rawText) { this.rawText = rawText; }

    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }
}
