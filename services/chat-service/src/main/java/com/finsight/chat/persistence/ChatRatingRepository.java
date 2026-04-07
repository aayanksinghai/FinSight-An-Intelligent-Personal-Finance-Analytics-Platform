package com.finsight.chat.persistence;

import com.finsight.chat.domain.ChatRating;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ChatRatingRepository extends JpaRepository<ChatRating, UUID> {
}
