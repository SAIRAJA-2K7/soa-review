package com.soareview.authservice.repository;

import com.soareview.authservice.entity.PasswordResetCredential;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

public interface PasswordResetCredentialRepository extends JpaRepository<PasswordResetCredential, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select credential from PasswordResetCredential credential where credential.tokenHash = :tokenHash")
    Optional<PasswordResetCredential> findForUpdate(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("delete from PasswordResetCredential credential where credential.userId = :userId")
    void deleteOutstandingForUser(@Param("userId") Long userId);

    @Modifying
    @Query("update PasswordResetCredential credential set credential.consumedAt = :consumedAt " +
            "where credential.tokenHash = :tokenHash and credential.expiresAt > :consumedAt " +
            "and credential.consumedAt is null")
    int consumeIfValid(@Param("tokenHash") String tokenHash, @Param("consumedAt") Instant consumedAt);

    @Modifying
    @Transactional
    @Query("update PasswordResetCredential credential set credential.expiresAt = :expiresAt " +
            "where credential.tokenHash = :tokenHash")
    int updateExpiry(@Param("tokenHash") String tokenHash, @Param("expiresAt") Instant expiresAt);
}
