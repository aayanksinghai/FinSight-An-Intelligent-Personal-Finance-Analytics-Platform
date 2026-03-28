package com.finsight.ingestion.parser;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses PDF bank statements using Apache PDFBox.
 *
 * Strategy:
 *  - Extract all text from the PDF
 *  - Detect table rows by matching date patterns
 *  - Parse each line as a transaction row using regex-based column extraction
 *
 * This is a heuristic parser — complex PDFs with non-selectable text (scanned images)
 * will not parse well. The parsing-ml-service (Phase 5) will handle advanced cases.
 */
@Component
public class PdfStatementParser {

    private static final Logger log = LoggerFactory.getLogger(PdfStatementParser.class);

    // Matches patterns like: 15/01/2024, 15-01-2024, 15 Jan 2024, Jan 15 2024
    private static final Pattern DATE_PATTERN = Pattern.compile(
        "\\b(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|\\d{1,2}[- ][A-Za-z]{3}[- ]\\d{2,4}|[A-Za-z]{3}\\s+\\d{1,2},?\\s+\\d{4})\\b"
    );

    // Matches amounts like: 1,234.56 or 12345.67 or 1234
    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
        "(?<![\\d.,])([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)(?![\\d.,])"
    );

    public ParseResult parse(InputStream inputStream, String fileName) throws Exception {
        List<ParsedRow> rows = new ArrayList<>();
        int totalAttempted = 0;

        PDDocument document = PDDocument.load(inputStream);
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setSortByPosition(true);
        String fullText = stripper.getText(document);
        document.close();

        String detectedBank = ColumnDetector.detectBank(fullText.substring(0, Math.min(2000, fullText.length())));
        log.info("PDF [{}] bank={} text length={}", fileName, detectedBank, fullText.length());

        String[] lines = fullText.split("\n");

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.length() < 10) continue;

            // A transaction line must have at least one date
            Matcher dateMatcher = DATE_PATTERN.matcher(trimmed);
            if (!dateMatcher.find()) continue;

            totalAttempted++;

            String rawDateStr = dateMatcher.group(0);
            Instant occurredAt = ColumnDetector.parseDate(rawDateStr);
            if (occurredAt == null) continue;

            // Remove the date from the line; remainder is description + amounts
            String remainder = trimmed.substring(dateMatcher.end()).trim();

            // Find all amounts in the remainder
            List<BigDecimal> amounts = new ArrayList<>();
            Matcher amtMatcher = AMOUNT_PATTERN.matcher(remainder);
            while (amtMatcher.find()) {
                BigDecimal amt = ColumnDetector.parseAmount(amtMatcher.group(1));
                if (amt != null) amounts.add(amt);
            }

            // Heuristic: last amount = balance, second-to-last = transaction amount
            BigDecimal debit = null, credit = null, balance = null;
            if (amounts.size() >= 2) {
                balance = amounts.get(amounts.size() - 1);
                BigDecimal txnAmt = amounts.get(amounts.size() - 2);
                // Determine debit vs credit from keywords in the line
                String upperLine = trimmed.toUpperCase();
                if (upperLine.contains(" CR") || upperLine.contains("CREDIT") || upperLine.contains(" CR ")) {
                    credit = txnAmt;
                } else {
                    debit = txnAmt;
                }
            } else if (amounts.size() == 1) {
                // Only one amount — assume transaction amount, guess debit
                debit = amounts.get(0);
            }

            // Description: text before first amount in remainder
            String rawDesc = remainder;
            Matcher firstAmt = AMOUNT_PATTERN.matcher(remainder);
            if (firstAmt.find()) {
                rawDesc = remainder.substring(0, firstAmt.start()).trim();
            }

            ParsedRow row = new ParsedRow();
            row.setOccurredAt(occurredAt);
            row.setRawDescription(rawDesc.isBlank() ? null : rawDesc);
            row.setMerchant(ColumnDetector.extractMerchant(rawDesc));
            row.setDebitAmount(debit);
            row.setCreditAmount(credit);
            row.setBalance(balance);
            row.setCurrency("INR");
            row.setRawText(trimmed);
            rows.add(row);
        }

        log.info("PDF [{}] parsed {} valid rows out of {} attempted", fileName, rows.size(), totalAttempted);
        return new ParseResult(rows, detectedBank, totalAttempted);
    }
}
