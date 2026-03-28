package com.finsight.ingestion.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface IngestionJobRepository extends MongoRepository<IngestionJobDocument, String> {

    /** All jobs owned by a user, newest first (Pageable handles sort). */
    Page<IngestionJobDocument> findByOwnerEmailOrderByCreatedAtDesc(String ownerEmail, Pageable pageable);

    /** Single job owned by a specific user (prevents cross-user access). */
    java.util.Optional<IngestionJobDocument> findByIdAndOwnerEmail(String id, String ownerEmail);
}
