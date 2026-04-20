package com.finsight.transaction.service;

import com.finsight.transaction.domain.Category;
import com.finsight.transaction.domain.Transaction;
import com.finsight.transaction.event.TransactionIngestedEvent;
import com.finsight.transaction.persistence.CategoryRepository;
import com.finsight.transaction.persistence.TransactionRepository;
import com.finsight.transaction.util.TransactionHashUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.UUID;

@Service
public class TransactionPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(TransactionPersistenceService.class);

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;

    public TransactionPersistenceService(TransactionRepository transactionRepository, CategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public Transaction saveIngestedTransaction(TransactionIngestedEvent event) {
        
        // Use either debit or credit amount for the hash
        java.math.BigDecimal amt = event.debitAmount() != null ? event.debitAmount() : event.creditAmount();
        String type = event.debitAmount() != null ? "DEBIT" : (event.creditAmount() != null ? "CREDIT" : "");
        
        String hash = TransactionHashUtil.generateHash(
            event.ownerEmail(), 
            event.occurredAt(), 
            amt, 
            type, 
            event.rawDescription()
        );

        if (transactionRepository.existsByContentHash(hash)) {
            log.info("Duplicate transaction detected for {} [Hash: {}]. Skipping save.", event.ownerEmail(), hash);
            return null; // Signals consumer it was a duplicate and should just skip
        }

        Category defaultCategory = categoryRepository.findByNameIgnoreCase("Uncategorized")
            .orElse(null);

        Transaction txn = new Transaction();
        txn.setId(UUID.randomUUID());
        txn.setContentHash(hash);
        txn.setOwnerEmail(event.ownerEmail());
        txn.setJobId(event.jobId());
        txn.setSourceFileName(event.sourceFileName());
        txn.setSourceBank(event.sourceBank());
        txn.setOccurredAt(event.occurredAt());
        txn.setRawDescription(event.rawDescription());
        txn.setNormalizedMerchant(event.merchant());
        txn.setCategory(defaultCategory);

        if (event.debitAmount() != null) {
            txn.setAmount(event.debitAmount());
            txn.setType("DEBIT");
        } else if (event.creditAmount() != null) {
            txn.setAmount(event.creditAmount());
            txn.setType("CREDIT");
        } else {
            return null;
        }

        txn.setCurrency(event.currency() != null ? event.currency() : "INR");
        txn.setBalanceAfter(event.balance());
        txn.setRawText(event.rawText());
        txn.setCreatedAt(Instant.now());
        txn.setUpdatedAt(Instant.now());

        return transactionRepository.save(txn);
    }
}
