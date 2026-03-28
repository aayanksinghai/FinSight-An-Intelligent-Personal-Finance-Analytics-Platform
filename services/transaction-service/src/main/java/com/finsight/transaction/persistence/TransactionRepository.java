package com.finsight.transaction.persistence;

import com.finsight.transaction.domain.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByIdAndOwnerEmail(UUID id, String ownerEmail);

    Page<Transaction> findByOwnerEmailOrderByOccurredAtDesc(String ownerEmail, Pageable pageable);

    @Query("SELECT t.type as type, SUM(t.amount) as total " +
           "FROM Transaction t " +
           "WHERE t.ownerEmail = :ownerEmail AND t.occurredAt >= :startDate AND t.occurredAt <= :endDate " +
           "GROUP BY t.type")
    List<Map<String, Object>> getSummaryByType(
        @Param("ownerEmail") String ownerEmail,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate
    );

    @Query("SELECT c.name as category, c.color as color, SUM(t.amount) as total " +
           "FROM Transaction t " +
           "JOIN t.category c " +
           "WHERE t.ownerEmail = :ownerEmail AND t.type = 'DEBIT' AND t.occurredAt >= :startDate AND t.occurredAt <= :endDate " +
           "GROUP BY c.name, c.color " +
           "ORDER BY total DESC")
    List<Map<String, Object>> getDebitSummaryByCategory(
        @Param("ownerEmail") String ownerEmail,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate
    );
}
