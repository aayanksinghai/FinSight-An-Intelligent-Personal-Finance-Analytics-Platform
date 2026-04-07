package com.finsight.chat.persistence;

import com.finsight.chat.domain.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {
    List<ChatSession> findByOwnerEmailOrderByCreatedAtDesc(String ownerEmail);
    Optional<ChatSession> findByIdAndOwnerEmail(UUID id, String ownerEmail);
}
