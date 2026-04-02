package com.finsight.transaction.service;

import com.finsight.transaction.domain.Category;
import com.finsight.transaction.domain.Transaction;
import com.finsight.transaction.event.TransactionCategorizedEvent;
import com.finsight.transaction.persistence.CategoryRepository;
import com.finsight.transaction.persistence.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionCategorizationService {

    private static final Logger log = LoggerFactory.getLogger(TransactionCategorizationService.class);

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;

    public TransactionCategorizationService(TransactionRepository transactionRepository, CategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public void updateTransactionCategory(Transaction txn, TransactionCategorizedEvent event) {
        // Find category by name
        if (event.categoryName() != null && !event.categoryName().isBlank()) {
            Category category = categoryRepository.findByNameIgnoreCase(event.categoryName())
                .orElse(null);
            
            if (category != null) {
                txn.setCategory(category);
            } else {
                log.warn("Category name '{}' from ML service not found in database. Keeping existing category.", event.categoryName());
            }
        }

        // Update merchant if provided by ML model
        if (event.merchant() != null && !event.merchant().isBlank()) {
            txn.setNormalizedMerchant(event.merchant());
        }

        transactionRepository.save(txn);
    }
}
