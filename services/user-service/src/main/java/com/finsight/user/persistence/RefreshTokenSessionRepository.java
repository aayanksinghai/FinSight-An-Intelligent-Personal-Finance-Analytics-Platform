package com.finsight.user.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RefreshTokenSessionRepository extends JpaRepository<RefreshTokenSession, UUID> {

    Optional<RefreshTokenSession> findByTokenJti(String tokenJti);

    @Modifying
    @Query("""
            update RefreshTokenSession r
            set r.revokedAt = :revokedAt
            where r.userEmail = :userEmail
              and r.revokedAt is null
              and r.expiresAt > :revokedAt
            """)
    int revokeActiveByUserEmail(String userEmail, Instant revokedAt);
}

