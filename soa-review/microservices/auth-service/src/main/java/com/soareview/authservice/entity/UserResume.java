package com.soareview.authservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_resumes")
public class UserResume {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_base64", nullable = false, length = 7_000_000)
    private String contentBase64;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentBase64() {
        return contentBase64;
    }

    public void setContentBase64(String contentBase64) {
        this.contentBase64 = contentBase64;
    }
}
