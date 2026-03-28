package com.finsight.ingestion.parser;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.enums.CSVReaderNullFieldIndicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses CSV bank statements.
 *
 * Strategy:
 * 1. Scan rows until a header row is found (contains a date-like column).
 * 2. Detect column indices using {@link ColumnDetector}.
 * 3. Parse each data row into a {@link ParsedRow}.
 *
 * Handles:
 *  - Metadata rows above the actual header (common in HDFC, ICICI exports)
 *  - Debit/credit as separate columns OR a single Amount + Type column
 *  - Comma-separated values with quoted fields
 */
@Component
public class CsvStatementParser {

    private static final Logger log = LoggerFactory.getLogger(CsvStatementParser.class);

    /** Maximum rows to scan looking for the header. */
    private static final int MAX_HEADER_SCAN = 15;

    public ParseResult parse(InputStream inputStream, String fileName) throws Exception {
        List<ParsedRow> rows = new ArrayList<>();
        int totalAttempted = 0;
        String detectedBank = "Unknown";

        try (CSVReader reader = new CSVReaderBuilder(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
            .withFieldAsNull(CSVReaderNullFieldIndicator.EMPTY_SEPARATORS)
            .build()) {

            List<String[]> allRows = reader.readAll();
            if (allRows.isEmpty()) {
                return new ParseResult(rows, detectedBank, 0);
            }

            // Detect bank from first few rows
            StringBuilder sb = new StringBuilder();
            allRows.stream().limit(5).forEach(r -> sb.append(String.join(" ", r)).append(" "));
            detectedBank = ColumnDetector.detectBank(sb.toString());

            // Find header row
            int headerIdx = -1;
            String[] headers = null;
            for (int i = 0; i < Math.min(MAX_HEADER_SCAN, allRows.size()); i++) {
                String[] candidate = ColumnDetector.normalizeHeaders(allRows.get(i));
                if (ColumnDetector.findDateColumn(candidate) >= 0) {
                    headers = candidate;
                    headerIdx = i;
                    break;
                }
            }

            if (headers == null) {
                log.warn("No header row found in CSV file: {}", fileName);
                return new ParseResult(rows, detectedBank, 0);
            }

            // Resolve column indices
            int dateCol   = ColumnDetector.findDateColumn(headers);
            int descCol   = ColumnDetector.findDescriptionColumn(headers);
            int debitCol  = ColumnDetector.findDebitColumn(headers);
            int creditCol = ColumnDetector.findCreditColumn(headers);
            int amtCol    = ColumnDetector.findAmountColumn(headers);
            int typeCol   = ColumnDetector.findTypeColumn(headers);
            int balCol    = ColumnDetector.findBalanceColumn(headers);

            log.info("CSV [{}] bank={} headerRow={} date={} desc={} debit={} credit={} amt={} type={} bal={}",
                fileName, detectedBank, headerIdx, dateCol, descCol, debitCol, creditCol, amtCol, typeCol, balCol);

            // Parse data rows
            for (int i = headerIdx + 1; i < allRows.size(); i++) {
                String[] raw = allRows.get(i);
                if (ColumnDetector.isSkippableRow(raw)) continue;

                totalAttempted++;
                String rawText = ColumnDetector.toRawText(raw);

                Instant occurredAt = ColumnDetector.parseDate(ColumnDetector.cell(raw, dateCol));
                if (occurredAt == null) continue; // Skip rows without a parseable date

                String rawDesc = ColumnDetector.cell(raw, descCol);
                BigDecimal debit  = debitCol  >= 0 ? ColumnDetector.parseAmount(ColumnDetector.cell(raw, debitCol))  : null;
                BigDecimal credit = creditCol >= 0 ? ColumnDetector.parseAmount(ColumnDetector.cell(raw, creditCol)) : null;
                BigDecimal balance = balCol >= 0 ? ColumnDetector.parseAmount(ColumnDetector.cell(raw, balCol)) : null;

                // Single amount column with type indicator
                if (debit == null && credit == null && amtCol >= 0) {
                    BigDecimal amt = ColumnDetector.parseAmount(ColumnDetector.cell(raw, amtCol));
                    String type = ColumnDetector.cell(raw, typeCol).toUpperCase();
                    if (type.contains("CR") || type.contains("CREDIT")) {
                        credit = amt;
                    } else {
                        debit = amt;
                    }
                }

                ParsedRow row = new ParsedRow();
                row.setOccurredAt(occurredAt);
                row.setRawDescription(rawDesc.isBlank() ? null : rawDesc);
                row.setMerchant(ColumnDetector.extractMerchant(rawDesc));
                row.setDebitAmount(debit);
                row.setCreditAmount(credit);
                row.setBalance(balance);
                row.setCurrency("INR");
                row.setRawText(rawText);
                rows.add(row);
            }
        }

        log.info("CSV [{}] parsed {} valid rows out of {} attempted", fileName, rows.size(), totalAttempted);
        return new ParseResult(rows, detectedBank, totalAttempted);
    }
}
