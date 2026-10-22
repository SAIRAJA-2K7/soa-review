package com.soareview.authservice.repository;

import com.soareview.authservice.entity.AuthAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthAuditEventRepository extends JpaRepository<AuthAuditEvent, String> {
}
