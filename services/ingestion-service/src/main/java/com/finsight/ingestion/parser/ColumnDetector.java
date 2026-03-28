package com.finsight.ingestion.parser;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Shared utilities for column-header detection and value parsing.
 *
 * Handles the most common Indian bank statement column naming conventions:
 * HDFC, ICICI, SBI, Axis, Kotak, IDFC, Yes Bank, and generic formats.
 */
public final class ColumnDetector {

    private ColumnDetector() {}

    // ── Column-index resolution ──────────────────────────────────────────────

    /**
     * Identifies the column index for a given semantic field from a header row.
     * Returns -1 if not found.
     */
    public static int findDateColumn(String[] headers) {
        return findFirst(headers, "date", "txn date", "transaction date", "value date",
            "posting date", "trans date");
    }

    public static int findDescriptionColumn(String[] headers) {
        return findFirst(headers, "description", "narration", "particulars", "details",
            "transaction details", "remarks", "transaction narration", "transaction description");
    }

    public static int findDebitColumn(String[] headers) {
        return findFirst(headers, "debit", "withdrawal", "dr", "dr amount",
            "debit amount", "withdrawal amt", "withdrawals");
    }

    public static int findCreditColumn(String[] headers) {
        return findFirst(headers, "credit", "deposit", "cr", "cr amount",
            "credit amount", "deposit amt", "deposits");
    }

    public static int findAmountColumn(String[] headers) {
        return findFirst(headers, "amount", "amt", "transaction amount");
    }

    public static int findTypeColumn(String[] headers) {
        return findFirst(headers, "type", "txn type", "dr/cr", "debit/credit", "cr/dr");
    }

    public static int findBalanceColumn(String[] headers) {
        return findFirst(headers, "balance", "closing balance", "running balance", "available balance");
    }

    private static int findFirst(String[] headers, String... candidates) {
        List<String> candidateList = Arrays.asList(candidates);
        for (int i = 0; i < headers.length; i++) {
            if (candidateList.contains(headers[i].trim().toLowerCase(Locale.ROOT))) {
                return i;
            }
        }
        // Partial match fallback
        for (int i = 0; i < headers.length; i++) {
            String h = headers[i].trim().toLowerCase(Locale.ROOT);
            for (String c : candidates) {
                if (h.contains(c)) return i;
            }
        }
        return -1;
    }

    // ── Bank detection ───────────────────────────────────────────────────────

    /** Detects the bank from header row content (heuristic). */
    public static String detectBank(String headerLine) {
        if (headerLine == null) return "Unknown";
        String lower = headerLine.toLowerCase(Locale.ROOT);
        if (lower.contains("hdfc")) return "HDFC Bank";
        if (lower.contains("icici")) return "ICICI Bank";
        if (lower.contains("state bank") || lower.contains("sbi")) return "SBI";
        if (lower.contains("axis")) return "Axis Bank";
        if (lower.contains("kotak")) return "Kotak Mahindra Bank";
        if (lower.contains("idfc")) return "IDFC First Bank";
        if (lower.contains("yes bank")) return "Yes Bank";
        if (lower.contains("pnb") || lower.contains("punjab")) return "Punjab National Bank";
        if (lower.contains("canara")) return "Canara Bank";
        if (lower.contains("union bank")) return "Union Bank";
        if (lower.contains("bank of baroda") || lower.contains("bob")) return "Bank of Baroda";
        if (lower.contains("indusind")) return "IndusInd Bank";
        if (lower.contains("rbl")) return "RBL Bank";
        if (lower.contains("federal")) return "Federal Bank";
        return "Unknown";
    }

    // ── Value parsing ────────────────────────────────────────────────────────

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("d/M/yyyy"),
        DateTimeFormatter.ofPattern("d-M-yyyy"),
        DateTimeFormatter.ofPattern("dd/MM/yy"),
        DateTimeFormatter.ofPattern("dd-MM-yy"),
        DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH)
    );

    public static Instant parseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String cleaned = raw.trim().replaceAll("\\s{2,}", " ");
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(cleaned, fmt).atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
            } catch (DateTimeParseException ignored) {
                // Try next format
            }
        }
        return null;
    }

    public static BigDecimal parseAmount(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String cleaned = raw.trim()
            .replace(",", "")
            .replace("₹", "")
            .replace("INR", "")
            .replace("Rs.", "")
            .replace("Rs", "")
            .trim();
        if (cleaned.isEmpty() || cleaned.equals("-") || cleaned.equals("0.00") && raw.isBlank()) {
            return null;
        }
        try {
            BigDecimal value = new BigDecimal(cleaned);
            return value.compareTo(BigDecimal.ZERO) == 0 ? null : value.abs();
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Extracts a simple merchant name from a raw bank narration. */
    public static String extractMerchant(String rawDescription) {
        if (rawDescription == null || rawDescription.isBlank()) return null;
        // Strip common prefixes like UPI/, NEFT/, IMPS/
        String cleaned = rawDescription
            .replaceAll("(?i)^(upi|neft|imps|rtgs|nach|ach|atm|pos|dp|chq|clg)[/-]?\\s*", "")
            .replaceAll("(?i)\\s*REF\\s*\\d+.*$", "")
            .replaceAll("(?i)\\s*TRAN\\s*ID\\s*\\S+.*$", "")
            .trim();

        // Take first 40 chars as merchant
        return cleaned.length() > 40 ? cleaned.substring(0, 40).trim() : cleaned;
    }

    /** Safely gets a cell value by index, returning empty string if out of bounds. */
    public static String cell(String[] row, int idx) {
        if (idx < 0 || idx >= row.length) return "";
        return row[idx] == null ? "" : row[idx].trim();
    }

    /** Returns true if a row looks like a header or blank row to skip. */
    public static boolean isSkippableRow(String[] row) {
        if (row == null || row.length == 0) return true;
        long nonEmpty = Arrays.stream(row).filter(s -> s != null && !s.isBlank()).count();
        return nonEmpty < 2;
    }

    /** Joins all cells of a row for rawText storage. */
    public static String toRawText(String[] row) {
        return String.join(" | ", row);
    }

    /** Normalize header strings for comparison. */
    public static String[] normalizeHeaders(String[] raw) {
        List<String> normalized = new ArrayList<>();
        for (String h : raw) {
            normalized.add(h == null ? "" : h.trim().toLowerCase(Locale.ROOT));
        }
        return normalized.toArray(new String[0]);
    }
}
