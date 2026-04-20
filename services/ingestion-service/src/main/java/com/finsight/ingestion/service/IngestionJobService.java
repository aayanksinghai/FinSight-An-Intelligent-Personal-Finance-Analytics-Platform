package com.finsight.ingestion.service;

import com.finsight.ingestion.event.TransactionIngestedEvent;
import com.finsight.ingestion.parser.*;
import com.finsight.ingestion.persistence.IngestionJobDocument;
import com.finsight.ingestion.persistence.IngestionJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
public class IngestionJobService {

    private static final Logger log = LoggerFactory.getLogger(IngestionJobService.class);

    private final IngestionJobRepository jobRepository;
    private final CsvStatementParser csvParser;
    private final XlsxStatementParser xlsxParser;
    private final PdfStatementParser pdfParser;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${ingestion.kafka.topic:transactions.ingested}")
    private String kafkaTopic;

    public IngestionJobService(
            IngestionJobRepository jobRepository,
            CsvStatementParser csvParser,
            XlsxStatementParser xlsxParser,
            PdfStatementParser pdfParser,
            KafkaTemplate<String, Object> kafkaTemplate) {
        this.jobRepository = jobRepository;
        this.csvParser = csvParser;
        this.xlsxParser = xlsxParser;
        this.pdfParser = pdfParser;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Creates a PENDING job document and immediately returns its ID.
     * Actual parsing is delegated to {@link #processAsync} which runs in a
     * background thread pool.
     */
    public IngestionJobDocument createJob(String ownerEmail, MultipartFile file) {
        validateFile(file);
        
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (java.io.IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read file");
        }
        
        String fileHash = org.springframework.util.DigestUtils.md5DigestAsHex(fileBytes);
        String cleanEmail = ownerEmail.trim().toLowerCase(Locale.ROOT);

        if (jobRepository.existsByOwnerEmailAndFileHashAndStatusIn(
                cleanEmail, fileHash, List.of(IngestionJobDocument.Status.COMPLETED, IngestionJobDocument.Status.DUPLICATED))) {
            
            IngestionJobDocument job = IngestionJobDocument.create(
                cleanEmail,
                file.getOriginalFilename(),
                file.getSize(),
                file.getContentType()
            );
            job.setFileHash(fileHash);
            job.setStatus(IngestionJobDocument.Status.DUPLICATED);
            job.setCompletedAt(Instant.now());
            job.setErrorMessage("Statement is a duplicate of a previously uploaded file.");
            job = jobRepository.save(job);
            log.info("Duplicate ingestion job skipped: jobId={} fileHash={}", job.getId(), fileHash);
            return job;
        }

        IngestionJobDocument job = IngestionJobDocument.create(
            cleanEmail,
            file.getOriginalFilename(),
            file.getSize(),
            file.getContentType()
        );
        job.setFileHash(fileHash);
        job = jobRepository.save(job);
        log.info("Ingestion job created: jobId={} owner={} file={} size={}",
            job.getId(), ownerEmail, file.getOriginalFilename(), file.getSize());

        // Kick off async parse natively using memory byte array
        processAsync(job.getId(), ownerEmail, fileBytes, file.getOriginalFilename(), file.getContentType());
        return job;
    }

    /**
     * Asynchronously parses the uploaded file and publishes events to Kafka.
     * Updates job status to PROCESSING → COMPLETED | FAILED.
     */
    @Async("ingestionExecutor")
    public void processAsync(String jobId, String ownerEmail, byte[] fileBytes, String fileName, String contentType) {
        IngestionJobDocument job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            log.error("Job not found for async processing: {}", jobId);
            return;
        }

        job.setStatus(IngestionJobDocument.Status.PROCESSING);
        job.setStartedAt(Instant.now());
        jobRepository.save(job);

        try {
            ParseResult result = parseFile(fileBytes, fileName, contentType);

            job.setDetectedBank(result.getDetectedBank());
            job.setRowsTotal(result.getTotalRowsAttempted());

            List<ParsedRow> rows = result.getRows();
            
            java.util.List<String> hashes = new java.util.ArrayList<>();
            for (ParsedRow row : rows) {
                java.math.BigDecimal amt = row.getDebitAmount() != null ? row.getDebitAmount() : row.getCreditAmount();
                String type = row.getDebitAmount() != null ? "DEBIT" : (row.getCreditAmount() != null ? "CREDIT" : "");
                String hash = com.finsight.ingestion.util.TransactionHashUtil.generateHash(
                    ownerEmail,
                    row.getOccurredAt(),
                    amt,
                    type,
                    row.getRawDescription()
                );
                hashes.add(hash);
            }

            java.util.Set<String> dupesSet = new java.util.HashSet<>();
            if (!hashes.isEmpty()) {
                try {
                    org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
                    String url = "http://transaction-service:8083/api/internal/transactions/check-duplicates";
                    String[] existingDupes = restTemplate.postForObject(url, hashes, String[].class);
                    if (existingDupes != null) {
                        dupesSet.addAll(java.util.Arrays.asList(existingDupes));
                    }
                } catch (Exception ex) {
                    log.warn("Failed to check duplicates with transaction-service: {}", ex.getMessage());
                }
            }

            int published = 0;
            java.util.Set<String> localSeen = new java.util.HashSet<>();

            for (int i = 0; i < rows.size(); i++) {
                ParsedRow row = rows.get(i);
                String hash = hashes.get(i);
                
                if (dupesSet.contains(hash) || localSeen.contains(hash)) {
                    continue;
                }
                localSeen.add(hash);

                TransactionIngestedEvent event = TransactionIngestedEvent.of(
                    ownerEmail.trim().toLowerCase(Locale.ROOT),
                    jobId,
                    fileName,
                    row.getOccurredAt(),
                    row.getRawDescription(),
                    row.getMerchant(),
                    row.getDebitAmount(),
                    row.getCreditAmount(),
                    row.getBalance(),
                    row.getCurrency(),
                    result.getDetectedBank(),
                    row.getRawText()
                );
                kafkaTemplate.send(kafkaTopic, ownerEmail, event);
                published++;
            }

            job.setRowsParsed(published);
            job.setStatus(IngestionJobDocument.Status.COMPLETED);
            job.setCompletedAt(Instant.now());
            log.info("Job {} completed: {} rows published to {}", jobId, published, kafkaTopic);

        } catch (Exception e) {
            log.error("Job {} failed during parsing: {}", jobId, e.getMessage(), e);
            job.setStatus(IngestionJobDocument.Status.FAILED);
            job.setErrorMessage(truncate(e.getMessage(), 500));
            job.setCompletedAt(Instant.now());
        }

        jobRepository.save(job);
    }

    public Page<IngestionJobDocument> listJobs(String ownerEmail, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 50);
        return jobRepository.findByOwnerEmailOrderByCreatedAtDesc(
            ownerEmail.trim().toLowerCase(Locale.ROOT),
            PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
    }

    public Page<IngestionJobDocument> listAllJobs(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 50);
        return jobRepository.findAll(
            PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
    }

    public java.util.Map<String, Object> getJobStats() {
        long total = jobRepository.count();
        if (total == 0) {
            return java.util.Map.of("totalJobs", 0L, "successRate", 0.0);
        }
        long completed = jobRepository.findAll().stream()
                .filter(j -> j.getStatus() == IngestionJobDocument.Status.COMPLETED)
                .count();
        double successRate = Math.round((completed / (double) total) * 10000.0) / 100.0;
        return java.util.Map.of(
                "totalJobs", total,
                "completedJobs", completed,
                "failedJobs", total - completed,
                "successRate", successRate
        );
    }

    public IngestionJobDocument getJob(String ownerEmail, String jobId) {
        return jobRepository.findByIdAndOwnerEmail(jobId, ownerEmail.trim().toLowerCase(Locale.ROOT))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private ParseResult parseFile(byte[] fileBytes, String originalFilename, String mimeType) throws Exception {
        String name = (originalFilename != null ? originalFilename : "").toLowerCase(Locale.ROOT);
        String contentType = mimeType != null ? mimeType.toLowerCase(Locale.ROOT) : "";

        try (InputStream in = new java.io.ByteArrayInputStream(fileBytes)) {
            if (name.endsWith(".pdf") || contentType.contains("pdf")) {
                return pdfParser.parse(in, originalFilename);
            }
            if (name.endsWith(".xlsx") || contentType.contains("spreadsheetml")) {
                return xlsxParser.parse(in, originalFilename, false);
            }
            if (name.endsWith(".xls") || contentType.contains("ms-excel")) {
                return xlsxParser.parse(in, originalFilename, true);
            }
            // Default: CSV
            return csvParser.parse(in, originalFilename);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No file provided or file is empty");
        }
        long maxBytes = 20L * 1024 * 1024; // 20 MB
        if (file.getSize() > maxBytes) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "File exceeds maximum size of 20 MB");
        }
        String name = (file.getOriginalFilename() != null ? file.getOriginalFilename() : "").toLowerCase(Locale.ROOT);
        if (!name.endsWith(".csv") && !name.endsWith(".xls") && !name.endsWith(".xlsx") && !name.endsWith(".pdf")) {
            String ct = file.getContentType() != null ? file.getContentType() : "";
            if (!ct.contains("csv") && !ct.contains("excel") && !ct.contains("spreadsheetml") && !ct.contains("pdf")) {
                throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Unsupported file type. Please upload CSV, XLS, XLSX, or PDF");
            }
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }
}
