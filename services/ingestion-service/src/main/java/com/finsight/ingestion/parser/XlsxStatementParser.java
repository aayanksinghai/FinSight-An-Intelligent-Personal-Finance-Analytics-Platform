package com.finsight.ingestion.parser;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses XLS and XLSX bank statements using Apache POI.
 *
 * Handles:
 *  - Both legacy XLS and modern XLSX formats
 *  - Numeric date cells (Excel serial dates)
 *  - Numeric amount cells
 *  - Multiple sheets — uses the first non-empty sheet
 */
@Component
public class XlsxStatementParser {

    private static final Logger log = LoggerFactory.getLogger(XlsxStatementParser.class);
    private static final int MAX_HEADER_SCAN = 15;

    public ParseResult parse(InputStream inputStream, String fileName, boolean isXls) throws Exception {
        List<ParsedRow> rows = new ArrayList<>();
        int totalAttempted = 0;
        String detectedBank = "Unknown";

        Workbook workbook = isXls
            ? new HSSFWorkbook(inputStream)
            : new XSSFWorkbook(inputStream);

        // Use the first sheet with data
        Sheet sheet = null;
        for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
            Sheet candidate = workbook.getSheetAt(s);
            if (candidate.getPhysicalNumberOfRows() > 1) {
                sheet = candidate;
                break;
            }
        }
        if (sheet == null) {
            workbook.close();
            return new ParseResult(rows, detectedBank, 0);
        }

        // Detect bank from first few rows
        StringBuilder bankScan = new StringBuilder();
        int scanLimit = Math.min(5, sheet.getLastRowNum() + 1);
        for (int r = 0; r < scanLimit; r++) {
            Row row = sheet.getRow(r);
            if (row != null) bankScan.append(rowToString(row, workbook)).append(" ");
        }
        detectedBank = ColumnDetector.detectBank(bankScan.toString());

        // Find header row
        int headerIdx = -1;
        String[] headers = null;
        for (int r = 0; r <= Math.min(MAX_HEADER_SCAN, sheet.getLastRowNum()); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            String[] candidate = ColumnDetector.normalizeHeaders(rowToArray(row, workbook));
            if (ColumnDetector.findDateColumn(candidate) >= 0) {
                headers = candidate;
                headerIdx = r;
                break;
            }
        }

        if (headers == null) {
            log.warn("No header row found in XLSX file: {}", fileName);
            workbook.close();
            return new ParseResult(rows, detectedBank, 0);
        }

        int dateCol   = ColumnDetector.findDateColumn(headers);
        int descCol   = ColumnDetector.findDescriptionColumn(headers);
        int debitCol  = ColumnDetector.findDebitColumn(headers);
        int creditCol = ColumnDetector.findCreditColumn(headers);
        int amtCol    = ColumnDetector.findAmountColumn(headers);
        int typeCol   = ColumnDetector.findTypeColumn(headers);
        int balCol    = ColumnDetector.findBalanceColumn(headers);

        log.info("XLSX [{}] bank={} headerRow={} date={} desc={} debit={} credit={} bal={}",
            fileName, detectedBank, headerIdx, dateCol, descCol, debitCol, creditCol, balCol);

        DataFormatter formatter = new DataFormatter();

        for (int r = headerIdx + 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            String[] rawArray = rowToArray(row, workbook);
            if (ColumnDetector.isSkippableRow(rawArray)) continue;

            totalAttempted++;

            // Date: try numeric cell first (Excel serial date), then string
            Instant occurredAt = null;
            if (dateCol >= 0) {
                Cell dateCell = row.getCell(dateCol);
                if (dateCell != null) {
                    if (dateCell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(dateCell)) {
                        occurredAt = dateCell.getLocalDateTimeCellValue()
                            .toInstant(java.time.ZoneOffset.UTC);
                    } else {
                        occurredAt = ColumnDetector.parseDate(formatter.formatCellValue(dateCell));
                    }
                }
            }
            if (occurredAt == null) continue;

            String rawDesc = ColumnDetector.cell(rawArray, descCol);
            BigDecimal debit  = debitCol  >= 0 ? parseNumericOrString(row, debitCol,  rawArray, formatter) : null;
            BigDecimal credit = creditCol >= 0 ? parseNumericOrString(row, creditCol, rawArray, formatter) : null;
            BigDecimal balance = balCol >= 0 ? parseNumericOrString(row, balCol, rawArray, formatter) : null;

            if (debit == null && credit == null && amtCol >= 0) {
                BigDecimal amt = parseNumericOrString(row, amtCol, rawArray, formatter);
                String type = ColumnDetector.cell(rawArray, typeCol).toUpperCase();
                if (type.contains("CR") || type.contains("CREDIT")) credit = amt;
                else debit = amt;
            }

            ParsedRow parsed = new ParsedRow();
            parsed.setOccurredAt(occurredAt);
            parsed.setRawDescription(rawDesc.isBlank() ? null : rawDesc);
            parsed.setMerchant(ColumnDetector.extractMerchant(rawDesc));
            parsed.setDebitAmount(debit);
            parsed.setCreditAmount(credit);
            parsed.setBalance(balance);
            parsed.setCurrency("INR");
            parsed.setRawText(ColumnDetector.toRawText(rawArray));
            rows.add(parsed);
        }

        workbook.close();
        log.info("XLSX [{}] parsed {} valid rows out of {} attempted", fileName, rows.size(), totalAttempted);
        return new ParseResult(rows, detectedBank, totalAttempted);
    }

    private BigDecimal parseNumericOrString(Row row, int col, String[] rawArray, DataFormatter formatter) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            double v = cell.getNumericCellValue();
            if (v == 0.0) return null;
            return BigDecimal.valueOf(Math.abs(v));
        }
        return ColumnDetector.parseAmount(formatter.formatCellValue(cell));
    }

    private String[] rowToArray(Row row, Workbook wb) {
        DataFormatter fmt = new DataFormatter();
        int lastCell = row.getLastCellNum();
        String[] result = new String[lastCell];
        for (int c = 0; c < lastCell; c++) {
            Cell cell = row.getCell(c);
            result[c] = cell == null ? "" : fmt.formatCellValue(cell);
        }
        return result;
    }

    private String rowToString(Row row, Workbook wb) {
        return String.join(" ", rowToArray(row, wb));
    }
}
