package com.soareview.applicationservice.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "application_service_documents", uniqueConstraints =
        @UniqueConstraint(columnNames = {"collection_name", "record_key"}))
public class ApplicationDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "collection_name", nullable = false, length = 80)
    private String collectionName;

    @Column(name = "record_key", nullable = false, length = 320)
    private String recordKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    protected ApplicationDocument() {
    }

    public ApplicationDocument(String collectionName, String recordKey, String payload) {
        this.collectionName = collectionName;
        this.recordKey = recordKey;
        this.payload = payload;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public Long getId() {
        return id;
    }

    public String getRecordKey() {
        return recordKey;
    }

    public void setRecordKey(String recordKey) {
        this.recordKey = recordKey;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}