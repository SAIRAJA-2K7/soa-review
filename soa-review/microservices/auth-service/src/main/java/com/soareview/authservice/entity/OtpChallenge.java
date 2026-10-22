package com.soareview.authservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "auth_otp_challenges")
public class OtpChallenge {

    @Id
    @Column(name = "email_key", nullable = false, updatable = false, length = 320)
    private String email;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    protected OtpChallenge() {
    }

    public OtpChallenge(String email, String codeHash, Instant expiresAt, int failedAttempts) {
        this.email = email;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.failedAttempts = failedAttempts;
    }

    public String getEmail() { return email; }
    public String getCodeHash() { return codeHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public int getFailedAttempts() { return failedAttempts; }
    public void setCodeHash(String codeHash) { this.codeHash = codeHash; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }
}
