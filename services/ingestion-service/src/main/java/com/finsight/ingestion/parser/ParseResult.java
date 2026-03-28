package com.finsight.ingestion.parser;

import java.util.List;

/**
 * Result of parsing a bank statement file.
 */
public class ParseResult {

    private final List<ParsedRow> rows;
    private final String detectedBank;
    private final int totalRowsAttempted;

    public ParseResult(List<ParsedRow> rows, String detectedBank, int totalRowsAttempted) {
        this.rows = rows;
        this.detectedBank = detectedBank;
        this.totalRowsAttempted = totalRowsAttempted;
    }

    public List<ParsedRow> getRows() { return rows; }
    public String getDetectedBank() { return detectedBank; }
    public int getTotalRowsAttempted() { return totalRowsAttempted; }
}
