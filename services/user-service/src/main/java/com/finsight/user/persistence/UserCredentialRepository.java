package com.finsight.user.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCredentialRepository extends JpaRepository<UserCredential, UUID> {

    Optional<UserCredential> findByEmail(String email);

    Optional<UserCredential> findByEmailAndDeactivatedAtIsNull(String email);

    boolean existsByEmail(String email);

    long deleteByEmail(String email);

    Page<UserCredential> findAllByDeactivatedAtIsNull(Pageable pageable);

    Page<UserCredential> findAllByDeactivatedAtIsNotNull(Pageable pageable);
}
