package com.finsight.transaction.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TransactionRepository extends JpaRepository<TransactionRecord, UUID>, JpaSpecificationExecutor<TransactionRecord> {

    Optional<TransactionRecord> findByIdAndOwnerEmail(UUID id, String ownerEmail);
}

