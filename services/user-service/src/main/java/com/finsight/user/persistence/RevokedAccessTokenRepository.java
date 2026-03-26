package com.finsight.user.persistence;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RevokedAccessTokenRepository extends JpaRepository<RevokedAccessToken, String> {

    int deleteByExpiresAtBefore(Instant now);
}
